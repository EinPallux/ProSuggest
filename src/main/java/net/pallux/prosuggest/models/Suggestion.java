package net.pallux.prosuggest.models;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SerializableAs("Suggestion")
public class Suggestion implements ConfigurationSerializable {

    private String id;
    private String title;
    private String description;
    private UUID authorUUID;
    private String authorName;
    private LocalDateTime createdAt;
    private Set<UUID> upvotes;
    private Set<UUID> downvotes;
    private String adminResponse;

    // Constructor for new suggestions
    public Suggestion(String id, String title, String description, UUID authorUUID, String authorName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.authorUUID = authorUUID;
        this.authorName = authorName;
        this.createdAt = LocalDateTime.now();
        this.upvotes = new HashSet<>();
        this.downvotes = new HashSet<>();
        this.adminResponse = null;
    }

    // Constructor for deserialization
    public Suggestion(Map<String, Object> map) {
        this.id = (String) map.get("id");
        this.title = (String) map.get("title");
        this.description = (String) map.get("description");
        this.authorUUID = UUID.fromString((String) map.get("authorUUID"));
        this.authorName = (String) map.get("authorName");
        this.createdAt = LocalDateTime.parse((String) map.get("createdAt"));

        // Handle upvotes
        List<String> upvoteList = (List<String>) map.getOrDefault("upvotes", new ArrayList<>());
        this.upvotes = new HashSet<>();
        for (String uuid : upvoteList) {
            this.upvotes.add(UUID.fromString(uuid));
        }

        // Handle downvotes
        List<String> downvoteList = (List<String>) map.getOrDefault("downvotes", new ArrayList<>());
        this.downvotes = new HashSet<>();
        for (String uuid : downvoteList) {
            this.downvotes.add(UUID.fromString(uuid));
        }

        this.adminResponse = (String) map.get("adminResponse");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("description", description);
        map.put("authorUUID", authorUUID.toString());
        map.put("authorName", authorName);
        map.put("createdAt", createdAt.toString());

        // Serialize upvotes
        List<String> upvoteList = new ArrayList<>();
        for (UUID uuid : upvotes) {
            upvoteList.add(uuid.toString());
        }
        map.put("upvotes", upvoteList);

        // Serialize downvotes
        List<String> downvoteList = new ArrayList<>();
        for (UUID uuid : downvotes) {
            downvoteList.add(uuid.toString());
        }
        map.put("downvotes", downvoteList);

        if (adminResponse != null) {
            map.put("adminResponse", adminResponse);
        }

        return map;
    }

    // Voting methods
    public VoteResult vote(UUID playerUUID, VoteType voteType) {
        boolean hadUpvote = upvotes.contains(playerUUID);
        boolean hadDownvote = downvotes.contains(playerUUID);

        if (voteType == VoteType.UPVOTE) {
            if (hadUpvote) {
                upvotes.remove(playerUUID);
                return VoteResult.REMOVED;
            } else {
                upvotes.add(playerUUID);
                downvotes.remove(playerUUID); // Remove downvote if exists
                return VoteResult.UPVOTED;
            }
        } else {
            if (hadDownvote) {
                downvotes.remove(playerUUID);
                return VoteResult.REMOVED;
            } else {
                downvotes.add(playerUUID);
                upvotes.remove(playerUUID); // Remove upvote if exists
                return VoteResult.DOWNVOTED;
            }
        }
    }

    public boolean hasVoted(UUID playerUUID) {
        return upvotes.contains(playerUUID) || downvotes.contains(playerUUID);
    }

    public VoteType getVoteType(UUID playerUUID) {
        if (upvotes.contains(playerUUID)) return VoteType.UPVOTE;
        if (downvotes.contains(playerUUID)) return VoteType.DOWNVOTE;
        return null;
    }

    // Utility methods
    public int getScore() {
        return upvotes.size() - downvotes.size();
    }

    public String getFormattedDate() {
        return createdAt.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
    }

    public List<String> getWrappedDescription(int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] words = description.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getAuthorUUID() { return authorUUID; }
    public void setAuthorUUID(UUID authorUUID) { this.authorUUID = authorUUID; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Set<UUID> getUpvotes() { return new HashSet<>(upvotes); }
    public void setUpvotes(Set<UUID> upvotes) { this.upvotes = new HashSet<>(upvotes); }

    public Set<UUID> getDownvotes() { return new HashSet<>(downvotes); }
    public void setDownvotes(Set<UUID> downvotes) { this.downvotes = new HashSet<>(downvotes); }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public int getUpvoteCount() { return upvotes.size(); }
    public int getDownvoteCount() { return downvotes.size(); }

    // Enums
    public enum VoteType {
        UPVOTE, DOWNVOTE
    }

    public enum VoteResult {
        UPVOTED, DOWNVOTED, REMOVED
    }
}