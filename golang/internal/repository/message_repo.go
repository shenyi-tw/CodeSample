package repository

import (
	"context"
	"repo/common/cmodels"

	"gorm.io/gorm"
)

type MessageRepository interface {
	CreateMessage(ctx context.Context, msg *cmodels.PubSubMessage) error
	ListByTopic(ctx context.Context, topic string) ([]cmodels.PubSubMessage, error)
}

type messageRepoImpl struct {
	db *gorm.DB
}

func NewMessageRepository(db *gorm.DB) MessageRepository {
	return &messageRepoImpl{db: db}
}

func (r *messageRepoImpl) CreateMessage(ctx context.Context, msg *cmodels.PubSubMessage) error {
	return r.db.WithContext(ctx).Create(msg).Error
}

func (r *messageRepoImpl) ListByTopic(ctx context.Context, topic string) ([]cmodels.PubSubMessage, error) {
	var msgs []cmodels.PubSubMessage
	err := r.db.WithContext(ctx).Where("topic = ?", topic).Find(&msgs).Error
	return msgs, err
}
