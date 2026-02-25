package service

import (
	"context"
	"mes-service/internal/repository"
	"repo/common/cmodels"
	"repo/common/cpayloads"

	"github.com/google/uuid"
)

type NotificationService interface {
	RegisterDevice(ctx context.Context, req cpayloads.TokenRegisterRequest, merchantID uuid.UUID) error
	GetMerchantDevices(ctx context.Context, merchantID string) ([]cpayloads.TokenInfoResponse, error)
	CleanInvalidTokens(ctx context.Context, tokens []string) error
}

type notificationSvcImpl struct {
	repo repository.NotificationRepository
}

func NewNotificationService(repo repository.NotificationRepository) NotificationService {
	return &notificationSvcImpl{repo: repo}
}

func (s *notificationSvcImpl) RegisterDevice(ctx context.Context, req cpayloads.TokenRegisterRequest, merchantID uuid.UUID) error {

	notify := &cmodels.MerchantNotification{
		MerchantID: merchantID,
		FcmToken:   req.FcmToken,
		Platform:   req.Platform,
		IsEnabled:  true,
	}
	return s.repo.UpsertToken(ctx, notify)
}

func (s *notificationSvcImpl) GetMerchantDevices(ctx context.Context, merchantID string) ([]cpayloads.TokenInfoResponse, error) {
	mID, err := uuid.Parse(merchantID)
	if err != nil {
		return nil, err
	}
	return s.repo.GetActiveTokens(ctx, mID)
}

func (s *notificationSvcImpl) CleanInvalidTokens(ctx context.Context, tokens []string) error {
	return s.repo.BatchDisableTokens(ctx, tokens)
}
