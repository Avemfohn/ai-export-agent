package com.aiexportagent.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic stand-in for a real LLM — no HTTP call, no cost, no API key
 * required. Active by default ({@code app.ai.provider=mock} or unset).
 *
 * <p>Scoring: keyword overlap between the tenant's buyer criteria (combined
 * buyer_criteria + target_sectors + target_regions, see
 * {@code LeadScoringService#toRequest}) and the candidate's
 * sector/description/name, with an extra bonus when the candidate's sector
 * itself overlaps target_sectors — sector match is the single strongest
 * qualification signal available, and a plain bag-of-words count would
 * otherwise dilute a genuine sector match down to "one keyword among many."
 *
 * <p>Drafting: no LLM customization — literal {@code {{placeholder}}}
 * substitution into the base template's subject/body.
 *
 * <p>Both are proof-of-plumbing heuristics, not real AI judgments — the job
 * is to exercise the prompt-building → structured-result → DB-write
 * pipeline end-to-end without needing a provider key. Swap
 * {@code app.ai.provider} to {@code openai} or {@code anthropic} for the
 * real thing.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "from", "this", "that", "true", "false",
            "null", "min", "max", "usd", "a", "an", "of", "to", "in", "on");

    private final ObjectMapper objectMapper;

    public MockAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${app.ai.mock-base-score:20}")
    private int baseScore;

    @Value("${app.ai.mock-score-per-keyword:15}")
    private int scorePerKeyword;

    @Value("${app.ai.mock-sector-match-bonus:30}")
    private int sectorMatchBonus;

    @Override
    public AiScoringResult score(AiScoringRequest request) {
        Set<String> criteriaWords = wordsOf(request.buyerCriteriaJson());
        Set<String> candidateWords = wordsOf(
                request.sector() + " " + request.description() + " " + request.companyName());
        Set<String> sectorWords = wordsOf(request.sector());

        long overlap = criteriaWords.stream().filter(candidateWords::contains).count();
        boolean sectorMatch = sectorWords.stream().anyMatch(criteriaWords::contains);

        int score = baseScore + (int) overlap * scorePerKeyword + (sectorMatch ? sectorMatchBonus : 0);
        score = Math.max(0, Math.min(100, score));

        String rationale;
        if (sectorMatch) {
            rationale = "Company's sector (\"" + request.sector() + "\") matches the tenant's target "
                    + "sectors, plus " + overlap + " overall keyword overlap(s) with buyer criteria "
                    + "(mock heuristic, not a real LLM judgment).";
        } else if (overlap > 0) {
            rationale = "Matched " + overlap + " keyword(s) from buyer criteria against the company's "
                    + "sector/description/name, but no direct sector match (mock heuristic, not a real "
                    + "LLM judgment).";
        } else {
            rationale = "No overlap found between buyer criteria and this company's sector/description.";
        }

        return new AiScoringResult(score, rationale, "mock", null);
    }

    @Override
    public AiEmailDraftResult draftEmail(AiEmailDraftRequest request) {
        String subjectTemplate;
        String bodyTemplate;
        try {
            JsonNode node = objectMapper.readTree(request.baseTemplateJson());
            subjectTemplate = node.path("subject").asText("");
            bodyTemplate = node.path("body").asText("");
        } catch (Exception e) {
            throw new AiClientException(
                    "Could not parse base template JSON: " + request.baseTemplateJson(), e);
        }

        String contactFirstName = firstNameOf(request.contactFullName());
        String subject = substitutePlaceholders(subjectTemplate, request, contactFirstName);
        String body = substitutePlaceholders(bodyTemplate, request, contactFirstName);

        return new AiEmailDraftResult(subject, body, "mock", null);
    }

    private static String firstNameOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private static String substitutePlaceholders(
            String template, AiEmailDraftRequest request, String contactFirstName) {
        if (template == null) {
            return "";
        }
        return template
                .replace("{{companyName}}", nullToEmpty(request.companyName()))
                .replace("{{contactFirstName}}", contactFirstName)
                .replace("{{senderName}}", nullToEmpty(request.senderName()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Set<String> wordsOf(String text) {
        if (text == null) {
            return Set.of();
        }
        Set<String> words = new HashSet<>();
        Arrays.stream(WORD_SPLIT.split(text.toLowerCase()))
                .filter(w -> w.length() > 2 && !STOPWORDS.contains(w))
                .forEach(words::add);
        return words;
    }
}
