package controllers

import (
	"mes-service/internal/service"
	"net/http"
	"repo/common/constants/ecode"
	"repo/common/cpayloads"
	"repo/common/logger"
	"repo/common/utils"

	"github.com/gin-gonic/gin"
)

type NotificationController struct {
	svc service.NotificationService
}

func NewNotificationController(svc service.NotificationService) *NotificationController {
	return &NotificationController{svc: svc}
}

// RegisterToken godoc
// @Summary Register a merchant device token
// @Tags Notification
// @Accept json
// @Produce json
// @Param request body cpayloads.TokenRegisterRequest true "Registration Payload"
// @Success 200 {object} cpayloads.NotificationResponse
// @Router /api/message/save_token [post]
func (ctrl *NotificationController) RegisterToken(c *gin.Context) {
	merchantID, err := utils.GetMerchantID(c)
	if err != nil {
		// Log the unique error code and return unauthorized
		logger.Log.Errorw("Merchant ID extraction failed", "code", ecode.BFFErrHdlInvalidMerchant, "error", err)
		c.JSON(http.StatusUnauthorized, gin.H{
			"error_code": ecode.BFFErrHdlInvalidMerchant,
			"message":    "Unauthorized: Merchant ID missing",
		})
		return
	}
	var req cpayloads.TokenRegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if err := ctrl.svc.RegisterDevice(c.Request.Context(), req, merchantID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to register"})
		return
	}

	c.JSON(http.StatusOK, cpayloads.NotificationResponse{Message: "success"})
}

// GetMerchantDevices godoc
// @Summary Get all registered device tokens for a merchant
// @Tags Notification
// @Produce json
// @Param merchant_id query string true "Merchant ID"
// @Success 200 {object} []string
// @Router /inside/api/message/get_tokens [get]
func (ctrl *NotificationController) GetMerchantDevices(c *gin.Context) {
	merchantID := c.Query("merchant_id")
	if merchantID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "merchant_id is required"})
		return
	}

	tokens, err := ctrl.svc.GetMerchantDevices(c.Request.Context(), merchantID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to fetch devices"})
		return
	}
	logger.Log.Debugw("GetMerchantDevices",
		"tokens", tokens)

	c.JSON(http.StatusOK, tokens)
}

// CleanInvalidTokens godoc
// @Summary Remove expired or invalid tokens from the system
// @Tags Notification
// @Accept json
// @Produce json
// @Param request body cpayloads.DisableTokensRequest true "List of invalid tokens"
// @Success 200 {object} cpayloads.NotificationResponse
// @Router /inside/api/message/disable_tokens [patch]
func (ctrl *NotificationController) CleanInvalidTokens(c *gin.Context) {
	var tokens cpayloads.DisableTokensRequest
	if err := c.ShouldBindJSON(&tokens); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if len(tokens.Tokens) == 0 {
		c.JSON(http.StatusOK, cpayloads.NotificationResponse{Message: "no tokens to clean"})
		return
	}

	if err := ctrl.svc.CleanInvalidTokens(c.Request.Context(), tokens.Tokens); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to clean tokens"})
		return
	}

	c.JSON(http.StatusOK, cpayloads.NotificationResponse{Message: "success"})
}
