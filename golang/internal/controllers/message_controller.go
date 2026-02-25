package controllers

import (
	"mes-service/internal/service"
	"net/http"
	"repo/common/cpayloads"

	"github.com/gin-gonic/gin"
)

type MessageController struct {
	svc service.MessageService
}

func NewMessageController(svc service.MessageService) *MessageController {
	return &MessageController{svc: svc}
}

// SaveMessage godoc
// @Summary Save PubSub Message
// @Tags PubSub
// @Accept json
// @Produce json
// @Param message body cpayloads.MessageCreateRequest true "Payload"
// @Success 201 {object} cpayloads.MessageResponse
// @Router /api/v1/messages [post]
func (ctrl *MessageController) SaveMessage(c *gin.Context) {
	var req cpayloads.MessageCreateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	resp, err := ctrl.svc.ProcessAndSave(c.Request.Context(), req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusCreated, resp)
}
