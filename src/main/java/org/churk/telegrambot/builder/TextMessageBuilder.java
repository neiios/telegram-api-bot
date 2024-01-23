package org.churk.telegrambot.builder;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class TextMessageBuilder {
    private final SendMessage message;
    public TextMessageBuilder(Long chatId) {
        this.message = new SendMessage();
        this.message.setChatId(String.valueOf(chatId));
    }

    public TextMessageBuilder withText(String text) {
        message.setText(text);
        return this;
    }

    public TextMessageBuilder withReplyToMessageId(Integer messageId) {
        message.setReplyToMessageId(messageId);
        return this;
    }

    public SendMessage build() {
        return message;
    }
}