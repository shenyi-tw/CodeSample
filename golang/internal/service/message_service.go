package service

import (
	"context"
	"encoding/json"
	"mes-service/internal/repository"
	"repo/common/cmodels"
	"repo/common/cpayloads"

	"gorm.io/datatypes"
)

type MessageService interface {
	ProcessAndSave(ctx context.Context, req cpayloads.MessageCreateRequest) (*cpayloads.MessageResponse, error)
}

type messageServiceImpl struct {
	repo repository.MessageRepository
}

func NewMessageService(repo repository.MessageRepository) MessageService {
	return &messageServiceImpl{repo: repo}
}

func (s *messageServiceImpl) ProcessAndSave(ctx context.Context, req cpayloads.MessageCreateRequest) (*cpayloads.MessageResponse, error) {
	// Marshal maps to JSON bytes
	rawData, err := json.Marshal(req.RawData)
	if err != nil {
		return nil, err
	}

	attrData, err := json.Marshal(req.Attributes)
	if err != nil {
		return nil, err
	}

	model := &cmodels.PubSubMessage{
		Topic:       req.Topic,
		RawData:     datatypes.JSON(rawData),
		Attributes:  datatypes.JSON(attrData),
		PublishedAt: req.PublishedAt, // No type assertion or re-marshaling needed
	}

	if err := s.repo.CreateMessage(ctx, model); err != nil {
		return nil, err
	}

	// Map model back to Response Payload
	return &cpayloads.MessageResponse{
		ID:        model.ID,
		Topic:     model.Topic,
		RawData:   req.RawData,
		CreatedAt: model.CreatedAt,
	}, nil
}
