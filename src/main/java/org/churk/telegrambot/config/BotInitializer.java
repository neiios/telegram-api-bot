package org.churk.telegrambot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.churk.telegrambot.service.TelegramBotService;
import org.churk.telegrambot.utility.DailyMessageLoader;
import org.churk.telegrambot.utility.FactLoader;
import org.churk.telegrambot.utility.StickerLoader;
import org.churk.telegrambot.utility.SubredditLoader;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotInitializer {
    private final TelegramBotService telegramBotService;
    private final StickerLoader stickerLoader;
    private final DailyMessageLoader dailyMessageLoader;
    private final FactLoader factLoader;
    private final SubredditLoader subredditLoader;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        stickerLoader.loadStickers();
        dailyMessageLoader.loadMessages();
        factLoader.loadFacts();
        subredditLoader.loadSubredits();
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBotService);
        } catch (TelegramApiException e) {
            log.error("Error while initializing the bot", e);
        }
    }
}
