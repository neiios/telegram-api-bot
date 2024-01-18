package org.churk.telegrampibot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.churk.telegrampibot.builder.MessageBuilder;
import org.churk.telegrampibot.config.MemeProperties;
import org.churk.telegrampibot.model.Sentence;
import org.churk.telegrampibot.model.Stats;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.Validable;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    protected static final Queue<Update> latestMessages = new CircularFifoQueue<>(3);
    private static final boolean ENABLED = true;
    private final MemeProperties memeProperties;
    private final MessageBuilder messageBuilder;
    private final StatsService statsService;
    private final StickerService stickerService;
    private final DailyMessageService dailyMessageService;
    private final FactService factService;
    private final MemeService memeService;

    public List<Validable> handleCommand(Update update) {
        Optional<Integer> messageIdToReply = Optional.of(update.getMessage().getMessageId());
        List<Validable> response = new ArrayList<>();
        List<String> commandList = processMessage(update);

        Map<Supplier<Optional<Validable>>, List<String>> commandHandlers = Map.of(
                // Process Random Fact
                () -> Optional.of(processRandomFact(update, Optional.empty())),
                List.of(".*/fact.*"),

                // Process Random Sticker
                () -> Optional.of(processRandomSticker(update, Optional.empty())),
                List.of(".*/sticker.*"),

                // Process Random Meme
                () -> Optional.ofNullable(processRandomMeme(commandList, update, Optional.empty()).get(0)),
                List.of(".*/meme.*"),

                // Create Register Message
                () -> Optional.of(messageBuilder.createRegisterMessage(update, messageIdToReply)),
                List.of(".*/pidorreg.*"),

                // Handle Stats
                () -> handleStats(commandList, update, Optional.empty()),
                List.of(".*/pidorstats.*"),

                // Create Stats Message for All
                () -> Optional.of(messageBuilder.createStatsMessageForAll(update, Optional.empty())),
                List.of(".*/pidorall.*"),

                // Create Stats Message for User
                () -> Optional.of(messageBuilder.createStatsMessageForUser(update, messageIdToReply)),
                List.of(".*/pidorme.*"),

                // Process Daily Winner Message
                () -> {
                    response.addAll(processDailyWinnerMessage());
                    return Optional.empty();
                },
                List.of(".*/pidor.*")
        );


        Optional<Supplier<Optional<Validable>>> commandHandler = commandHandlers.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(commandList.toString()::matches))
                .map(Map.Entry::getKey)
                .findFirst();

        commandHandler.orElse(this::processRandomSticker)
                .get()
                .ifPresent(response::add);

        return response;
    }

    public List<Validable> processRandomMeme(List<String> commandList, Update update, Optional<Integer> messageIdToReply) {
        Long chatId = update.getMessage().getChatId();
        String subreddit = (commandList.size() == 2) ? commandList.get(1) : null;

        Optional<File> memeFile = retrieveMeme(subreddit);
        if (memeFile.isPresent()) {
            return List.of(messageBuilder.createPhotoMessage(messageIdToReply, chatId, memeFile.get()));
        }

        log.error("Meme file was not downloaded in the given time");
        return List.of();
    }

    private Optional<File> retrieveMeme(String subreddit) {
        if (subreddit == null) {
            log.info("Sending random meme");
            return memeService.getMeme();
        }
        return memeService.getMemeFromSubreddit(subreddit);
    }


    public List<Validable> processScheduledRandomMeme() {
        assert latestMessages.peek() != null;
        String subreddit = memeProperties.getScheduledSubreddits().get(ThreadLocalRandom.current().nextInt(memeProperties.getScheduledSubreddits().size()));
        List<Validable> messages = new ArrayList<>();
        messages.add(messageBuilder.createMessage("Here's a random meme from subreddit: " + subreddit, latestMessages.peek().getMessage().getChatId(), latestMessages.peek().getMessage().getFrom().getFirstName(), Optional.empty()));
        messages.add(processRandomMeme(List.of(subreddit), latestMessages.peek(), Optional.empty()).get(0));
        return messages;
    }

    private Optional<Validable> handleStats(List<String> commandList, Update update, Optional<Integer> messageIdToReply) {
        if (commandList.isEmpty() || commandList.size() > 2) {
            log.error("Invalid command: {}", commandList);
            return Optional.empty();
        }
        int year = (commandList.size() == 2) ? Integer.parseInt(commandList.get(1)) : LocalDateTime.now().getYear();

        return Optional.of(messageBuilder.createStatsMessageForYear(update, year, messageIdToReply));
    }


    public List<String> processMessage(Update update) {
        String message = update.getMessage().getText();
        latestMessages.add(update);

        if (message.isBlank()) {
            log.info("Blank message received");
            return List.of();
        }

        return List.of(message.split(" "));
    }

    public List<Validable> processDailyWinnerMessage() {
        log.info("Scheduled message");
        List<Stats> allStats = statsService.getAllStats();

        if (allStats.isEmpty()) {
            log.info("No stats available to pick a winner.");
            return List.of();
        }

        if (statsService.existsByWinnerToday()) {
            return handleExistingWinner(allStats);
        }

        return handleNewWinner(allStats);
    }

    private List<Validable> handleExistingWinner(List<Stats> allStats) {
        Stats winner = allStats.stream().filter(Stats::getIsWinner).findFirst().orElse(null);
        if (winner == null) {
            log.error("Winner exists but not found in the database");
            return List.of();
        }
        String winnerExistsMessage = dailyMessageService.getKeyNameSentence("key_name") + winner.getFirstName();
        return messageBuilder.createMessages(List.of(winnerExistsMessage), winner.getChatId(), winner.getFirstName());
    }

    private List<Validable> handleNewWinner(List<Stats> allStats) {
        Stats winner = allStats.get(ThreadLocalRandom.current().nextInt(allStats.size()));
        if (ENABLED) {
            winner.setScore(winner.getScore() + 1);
            winner.setIsWinner(Boolean.TRUE);
            statsService.updateStats(winner);
        }
        List<String> sentenceList = new ArrayList<>(dailyMessageService.getRandomGroupSentences().stream().map(Sentence::getText).toList());
        if (sentenceList.isEmpty()) {
            return List.of();
        }
        int lastSentenceIndex = sentenceList.size() - 1;
        sentenceList.set(lastSentenceIndex, sentenceList.get(lastSentenceIndex) + winner.getFirstName());
        return messageBuilder.createMessages(sentenceList, winner.getChatId(), winner.getFirstName());
    }


    private Validable processRandomFact(Update update, Optional<Integer> messageIdToReply) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();
        String randomFact = factService.getRandomFact();
        log.info("Sending fact: {}", randomFact);

        return messageBuilder.createMessage(randomFact, chatId, firstName, messageIdToReply);
    }

    private Validable processRandomSticker(Update update, Optional<Integer> messageIdToReply) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();
        String stickerId = stickerService.getRandomStickerId();
        log.info("Sending sticker: {}", stickerId);

        return messageBuilder.createStickerMessage(stickerId, chatId, firstName, messageIdToReply);
    }

    private Optional<Validable> processRandomSticker() {
        if (ThreadLocalRandom.current().nextInt(100) > 2) {
            return Optional.empty();
        }
        assert latestMessages.peek() != null;
        Message message = latestMessages.peek().getMessage();
        Optional<Integer> messageIdToReply = Optional.of(message.getMessageId());

        Long chatId = message.getChatId();
        String firstName = message.getFrom().getFirstName();
        String stickerId = stickerService.getRandomStickerId();
        log.info("Sending sticker: {}", stickerId);

        return Optional.of(messageBuilder.createStickerMessage(stickerId, chatId, firstName, messageIdToReply));
    }

    public void resetWinner() {
        List<Stats> allStats = statsService.getAllStats();
        allStats.forEach(stats -> stats.setIsWinner(Boolean.FALSE));
        statsService.updateStats(allStats);
    }

}
