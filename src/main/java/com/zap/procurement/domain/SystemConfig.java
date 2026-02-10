package com.zap.procurement.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "system_configs")
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", nullable = false)
    private String key;

    @Column(name = "config_value")
    private String value;

    private String description;

    @Column(name = "config_group")
    private String group;

    @Column(nullable = false)
    private boolean active = true;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
