package cn.wangweisong.raft.common;

import lombok.Getter;

/**
 * @author wang
 * @date 2019/11/13 周三 上午11:54
 */
@Getter
public enum NodeStatus {
    /**
     * 节点身份状态枚举
     * FOLLOWER 跟随者，
     * CANDIDATE 候选者，
     * LEADER 领导者
     */
    FOLLOWER(0),
    CANDIDATE(1),
    LEADER(2);

    int code;

    NodeStatus(int code) {
        this.code = code;
    }

    public static NodeStatus value(int i) {
        for (NodeStatus value : NodeStatus.values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }
}
