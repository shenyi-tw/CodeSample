package main

import (
	"fmt"
	"log"
	"mes-service/internal/service"
	"os"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"

	"mes-service/internal/controllers"
	"mes-service/internal/repository"

	"repo/common/cmodels"
	"repo/common/config"
	"repo/common/constants"
	"repo/common/cpath"
	"repo/common/logger"
	"repo/common/utils"
)

var BuildDate = "No date provided"
var GitHash = "No git_hash provided"

func main() {
	NEONDB, _ := os.LookupEnv("NEONDBL")
	subName := "local"
	if strings.Compare(NEONDB, "true") == 0 {
		subName = "neondb"
	}

	logger.InitLogger(os.Getenv("APP_ENV"), "mes-service"+"-"+subName)
	defer logger.Log.Sync() // Cleanly flush logs on exit
	logger.Log.Info("mes-service 0.0.1 with common-" + constants.Version)
	logger.Log.Debugw("User login attempt",
		"username", "user",
		"module", "auth",
	)

	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, relying on environment variables.")
	}

	cfg := config.LoadConfig()

	log.Println("cfg.DBName", cfg.DBName)
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=disable",
		cfg.DBHost, cfg.DBUser, cfg.DBPassword, cfg.DBName, cfg.DBPort)
	if strings.Compare(NEONDB, "true") == 0 {
		dsn = fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=require channel_binding=require",
			cfg.DB_NEONDB_HOST, cfg.DB_NEONDB_USER,
			cfg.DB_NEONDB_PASSWORD, cfg.DB_NEONDB_NAME, cfg.DB_NEONDB_PORT)
	}
	log.Println("dsn", dsn[:10])

	db, err := utils.InitGorm(cfg, dsn)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Auto Migration
	db.AutoMigrate(
		&cmodels.PubSubMessage{},
		&cmodels.MerchantNotification{},
	)

	// Inside main()
	repo := repository.NewMessageRepository(db)
	svc := service.NewMessageService(repo)
	ctrl := controllers.NewMessageController(svc)

	repoNot := repository.NewNotificationRepo(db)
	svcNot := service.NewNotificationService(repoNot)
	ctrlNot := controllers.NewNotificationController(svcNot)

	r := gin.Default()

	// Swagger
	r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	// Routes
	inside := r.Group(cpath.MesInsidePathBase)
	{
		inside.POST(cpath.MesPathSaveMessage, ctrl.SaveMessage)
		inside.GET(cpath.MesPathGetTokens, ctrlNot.GetMerchantDevices)
		inside.PATCH(cpath.MesPathDisableTokens, ctrlNot.CleanInvalidTokens)
	}
	tokenPath := r.Group(cpath.MesPathBase)
	{
		tokenPath.POST(cpath.MesPathSaveToken, ctrlNot.RegisterToken)
	}

	r.Run(":8080")
}
