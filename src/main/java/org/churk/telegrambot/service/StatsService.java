package org.churk.telegrambot.service;

import lombok.RequiredArgsConstructor;
import org.churk.telegrambot.model.Stats;
import org.churk.telegrambot.repository.StatsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsRepository statsRepository;
    /**
     * Get all stats from all years and filter by chatId
     *
     * @param chatId
     * @return List<Stats>
     */
    public List<Stats> getAggregatedStatsByChatId(Long chatId) {
        return statsRepository.findAll().stream()
                .filter(stats -> stats.getChatId().equals(chatId))
                .collect(Collectors.groupingBy(Stats::getUserId))
                .entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    UUID statsId = entry.getValue().get(0).getStatsId();
                    String firstName = entry.getValue().get(0).getFirstName();
                    LocalDateTime createdAt = entry.getValue().get(0).getCreatedAt();
                    Boolean isWinner = entry.getValue().get(0).getIsWinner();
                    long totalScore = entry.getValue().stream().mapToLong(Stats::getScore).sum();
                    return new Stats(statsId, chatId, userId, firstName, totalScore, createdAt, isWinner);
                })
                .toList();
    }

    public List<Stats> getStatsByChatIdAndYear(Long chatId, int year) {
        return statsRepository.findStatsByChatIdAndYear(chatId, year);
    }

    public List<Stats> getStatsByChatIdAndUserId(Long chatId, Long userId) {
        return statsRepository.findStatsByChatIdAndUserId(chatId, userId);
    }

    public boolean existsByUserId(Long userId) {
        return statsRepository.existsByUserId(userId);
    }

    public boolean existsByWinnerToday() {
        return statsRepository.existsIsWinner();
    }

    public List<Stats> getAllStats() {
        return statsRepository.findAll();
    }

    public void addStat(Stats stats) {
        statsRepository.save(stats);
    }

    public void updateStats(Stats stats) {
        statsRepository.save(stats);
    }

    public void updateStats(List<Stats> stats) {
        statsRepository.saveAll(stats);
    }
}
