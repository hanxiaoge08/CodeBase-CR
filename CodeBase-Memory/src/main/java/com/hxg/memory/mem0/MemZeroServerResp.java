package com.hxg.memory.mem0;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemZeroServerResp {

    // 关系数据
    private List<MemZeroResults> results;

    // 关系数据
    private List<MemZeroRelation> relations;

    public List<MemZeroResults> getResults() {
        return results;
    }

    public void setResults(List<MemZeroResults> results) {
        this.results = results;
    }

    public List<MemZeroRelation> getRelations() {
        return relations;
    }

    public void setRelations(List<MemZeroRelation> relations) {
        this.relations = relations;
    }

    /**
     * Mem0 关系数据模型
     * 对应 Mem0 服务返回的 relations 数组中的每个关系对象
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemZeroRelation {
        private String source;        // 源节点
        private String relationship;  // 关系类型
        private String target;        // 目标路径
        private String destination;   // 目的地
        
        @Override
        public String toString() {
            return "MemZeroRelation{" +
                    "source='" + source + '\'' +
                    ", relationship='" + relationship + '\'' +
                    ", target='" + target + '\'' +
                    ", destination='" + destination + '\'' +
                    '}';
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemZeroResults {
        private String id;
        private String memory; // 实际的记忆内容
        private String hash;
        private Map<String, Object> metadata;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("created_at")
        private ZonedDateTime createdAt;
        
        @JsonProperty("updated_at")
        private ZonedDateTime updatedAt;
        
        @JsonProperty("agent_id")
        private String agentId;
        
        @JsonProperty("run_id")
        private String runId;

        @JsonProperty("score")
        private Double score;

        @JsonProperty("role")
        private String role;

        @Override
        public String toString() {
            return "MemZeroResults{" +
                    "id='" + id + '\'' +
                    ", memory='" + memory + '\'' +
                    ", hash='" + hash + '\'' +
                    ", metadata=" + metadata +
                    ", userId='" + userId + '\'' +
                    ", createdAt=" + createdAt +
                    ", updatedAt=" + updatedAt +
                    ", agentId='" + agentId + '\'' +
                    ", runId='" + runId + '\'' +
                    ", score=" + score +
                    ", role='" + role + '\'' +
                    '}';
        }
    }

}
