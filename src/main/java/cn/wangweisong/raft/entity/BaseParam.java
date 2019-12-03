package cn.wangweisong.raft.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author wang
 * @date 2019/11/13 周三 下午1:14
 */
@Setter
@Getter
public class BaseParam implements Serializable {
    /** 任期编号 */
    protected long term;

    /** 请求目标ID（IP） */
    protected String serverId;
}
