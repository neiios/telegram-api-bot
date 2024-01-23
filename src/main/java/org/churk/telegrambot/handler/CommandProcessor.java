package org.churk.telegrambot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.churk.telegrambot.config.BotProperties;
import org.churk.telegrambot.factory.HandlerFactory;
import org.churk.telegrambot.model.Command;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.interfaces.Validable;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandProcessor {
    // protected static final Queue<Update> latestMessages = new CircularFifoQueue<>(3);
    private final HandlerFactory handlerFactory;
    private final BotProperties botProperties;

    public List<Validable> handleCommand(Update update) {
        List<String> arguments = List.of(update.getMessage().getText().split(" "));
        String commandName = arguments.getFirst().formatted(botProperties.getWinnerName());
        Command command = Command.getCommand(commandName);
        arguments.removeFirst();

        CommandHandler handler = handlerFactory.getHandler(command, arguments);
        return handler.handle(update);
    }
//
//    private Optional<Validable> processRandomResponse() {
//        List<Supplier<Optional<Validable>>> randomResponseHandlers = List.of(
//                this::processRandomSticker,
//                this::processRandomFact
//        );
//
//        if (ThreadLocalRandom.current().nextInt(100) > 1) {
//            return Optional.empty();
//        }
//        int randomIndex = ThreadLocalRandom.current().nextInt(randomResponseHandlers.size());
//        return randomResponseHandlers.get(randomIndex).get();
//    }
//
//    private Optional<Validable> processRandomFact() {
//        assert latestMessages.peek() != null;
//        return Optional.of(processRandomFact(latestMessages.peek(), Optional.of(latestMessages.peek().getMessage().getMessageId())));
//    }
//
//    public List<Validable> processRandomMeme(List<String> commandList, Update update, Optional<Integer> messageIdToReply) {
//        Long chatId = update.getMessage().getChatId();
//        String subreddit = (commandList.size() == 2) ? commandList.get(1) : null;
//        Optional<File> memeFile;
//        try {
//            memeFile = retrieveMeme(subreddit);
//            if (memeFile.isPresent()) {
//                Optional<String> caption = Optional.of("From r/%s".formatted(subreddit));
//                String filename = memeFile.get().getName().toLowerCase();
//                Optional<String> isSubreddit = subreddit == null ? Optional.empty() : caption;
//                return filename.endsWith(".gif") ?
//                        List.of(messageBuilder.createAnimationMessage(messageIdToReply, chatId, memeFile.get(), isSubreddit)) :
//                        List.of(messageBuilder.createPhotoMessage(messageIdToReply, chatId, memeFile.get(), isSubreddit));
//            }
//        } catch (feign.FeignException.NotFound e) {
//            log.error("Subreddit not found: {}", e.getMessage());
//            return List.of(messageBuilder.createMessage("Subreddit does not exist", chatId, update.getMessage().getFrom().getFirstName(), messageIdToReply));
//        } catch (Exception e) {
//            log.error("Error fetching meme from subreddit: {}", e.getMessage());
//            return List.of(messageBuilder.createMessage("Error fetching meme from subreddit", chatId, update.getMessage().getFrom().getFirstName(), messageIdToReply));
//        }
//        return List.of();
//    }
//
//    private Optional<File> retrieveMeme(String subreddit) throws feign.FeignException.NotFound {
//        if (subreddit != null) {
//            return redditService.getMemeFromSubreddit(subreddit);
//        }
//            log.info("Sending random meme");
//            return redditService.getMeme();
//    }
//
//    public List<Validable> processScheduledRandomRedditMeme() {
//        assert latestMessages.peek() != null;
//        String subreddit = subredditService.getSubreddits().get(ThreadLocalRandom.current().nextInt(subredditService.getSubreddits().size())).getName();
//        return List.of(processRandomMeme(List.of(subreddit, subreddit), latestMessages.peek(), Optional.empty()).get(0));
//    }
//
//    private Optional<Validable> handleStats(List<String> commandList, Update update, Optional<Integer> messageIdToReply) {
//        if (commandList.isEmpty() || commandList.size() > 2) {
//            log.error("Invalid command: {}", commandList);
//            return Optional.empty();
//        }
//        int year = (commandList.size() == 2) ? Integer.parseInt(commandList.get(1)) : LocalDateTime.now().getYear();
//
//        return Optional.of(messageBuilder.createStatsMessageForYear(update, year, messageIdToReply));
//    }
//
//
//    public List<String> processMessage(Update update) {
//        String message = update.getMessage().getText();
//        latestMessages.add(update);
//
//        if (message.isBlank()) {
//            log.info("Blank message received");
//            return List.of();
//        }
//
//        return List.of(message.split(" "));
//    }
//
//    public List<Validable> processDailyWinnerMessage() {
//        log.info("Scheduled message");
//        List<Stats> allStats = statsService.getAllStats();
//
//        if (allStats.isEmpty()) {
//            log.info("No stats available to pick a winner.");
//            return List.of();
//        }
//
//        if (statsService.existsByWinnerToday()) {
//            return handleExistingWinner(allStats);
//        }
//
//        return handleNewWinner(allStats);
//    }
//
//    private List<Validable> handleExistingWinner(List<Stats> allStats) {
//        Stats winner = allStats.stream().filter(Stats::getIsWinner).findFirst().orElse(null);
//        if (winner == null) {
//            log.error("Winner exists but not found in the database");
//            return List.of();
//        }
//        String winnerExistsMessage = dailyMessageService.getKeyNameSentence("key_name").formatted(botProperties.getWinnerName(), winner.getFirstName());
//        return messageBuilder.createMessages(List.of(winnerExistsMessage), winner.getChatId(), winner.getFirstName());
//    }
//
//    private List<Validable> handleNewWinner(List<Stats> allStats) {
//        Stats winner = allStats.get(ThreadLocalRandom.current().nextInt(allStats.size()));
//        if (ENABLED) {
//            winner.setScore(winner.getScore() + 1);
//            winner.setIsWinner(Boolean.TRUE);
//            statsService.updateStats(winner);
//        }
//        List<String> sentenceList = new ArrayList<>(dailyMessageService.getRandomGroupSentences().stream()
//                .map(sentence -> sentence.getText()
//                        .formatted(botProperties.getWinnerName()))
//                .toList());
//        if (sentenceList.isEmpty()) {
//            return List.of();
//        }
//        int lastSentenceIndex = sentenceList.size() - 1;
//        sentenceList.set(lastSentenceIndex, sentenceList.get(lastSentenceIndex) + winner.getFirstName());
//        return messageBuilder.createMessages(sentenceList, winner.getChatId(), winner.getFirstName());
//    }
//
//    private Validable processRandomFact(Update update, Optional<Integer> messageIdToReply) {
//        Long chatId = update.getMessage().getChatId();
//        String firstName = update.getMessage().getFrom().getFirstName();
//        String randomFact = factService.getRandomFact();
//
//        return messageBuilder.createMessage(Objects.requireNonNullElse(randomFact, "No facts found in database"), chatId, firstName, messageIdToReply);
//    }
//
//    private Validable processRandomSticker(Update update, Optional<Integer> messageIdToReply) {
//        Long chatId = update.getMessage().getChatId();
//        String firstName = update.getMessage().getFrom().getFirstName();
//        String stickerId = stickerService.getRandomStickerId();
//
//        return messageBuilder.createStickerMessage(stickerId, chatId, firstName, messageIdToReply);
//    }
//
//    private Optional<Validable> processRandomSticker() {
//        assert latestMessages.peek() != null;
//        Message message = latestMessages.peek().getMessage();
//        Optional<Integer> messageIdToReply = Optional.of(message.getMessageId());
//
//        Long chatId = message.getChatId();
//        String firstName = message.getFrom().getFirstName();
//        String stickerId = stickerService.getRandomStickerId();
//        log.info("Sending sticker: {}", stickerId);
//
//        return Optional.of(messageBuilder.createStickerMessage(stickerId, chatId, firstName, messageIdToReply));
//    }
//
//    public void resetWinner() {
//        List<Stats> allStats = statsService.getAllStats();
//        allStats.forEach(stats -> stats.setIsWinner(Boolean.FALSE));
//        statsService.updateStats(allStats);
//    }
}