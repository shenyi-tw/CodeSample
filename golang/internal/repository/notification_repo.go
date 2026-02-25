package repository

import (
	"context"
	"repo/common/cmodels"
	"repo/common/cpayloads"

	"github.com/google/uuid"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type NotificationRepository interface {
	UpsertToken(ctx context.Context, notify *cmodels.MerchantNotification) error
	GetActiveTokens(ctx context.Context, merchantID uuid.UUID) ([]cpayloads.TokenInfoResponse, error)
	DisableToken(ctx context.Context, token string) error
	BatchDisableTokens(ctx context.Context, tokens []string) error
}

type notificationRepoImpl struct {
	db *gorm.DB
}

func NewNotificationRepo(db *gorm.DB) NotificationRepository {
	return &notificationRepoImpl{db: db}
}

func (r *notificationRepoImpl) UpsertToken(ctx context.Context, notify *cmodels.MerchantNotification) error {
	return r.db.WithContext(ctx).Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "merchant_id"}, {Name: "fcm_token"}},
		DoUpdates: clause.AssignmentColumns([]string{"is_enabled", "platform", "updated_at"}),
	}).Create(notify).Error
}

func (r *notificationRepoImpl) GetActiveTokens(ctx context.Context, merchantID uuid.UUID) ([]cpayloads.TokenInfoResponse, error) {
	var results []cpayloads.TokenInfoResponse
	err := r.db.WithContext(ctx).
		Model(&cmodels.MerchantNotification{}).
		Select("fcm_token, platform").
		Where("merchant_id = ? AND is_enabled = ?", merchantID, true).
		Scan(&results).Error
	return results, err
}

func (r *notificationRepoImpl) DisableToken(ctx context.Context, token string) error {
	return r.db.WithContext(ctx).Model(&cmodels.MerchantNotification{}).
		Where("fcm_token = ?", token).Update("is_enabled", false).Error
}

func (r *notificationRepoImpl) BatchDisableTokens(ctx context.Context, tokens []string) error {
	if len(tokens) == 0 {
		return nil
	}
	return r.db.WithContext(ctx).Model(&cmodels.MerchantNotification{}).
		Where("fcm_token IN ?", tokens).Update("is_enabled", false).Error
}
