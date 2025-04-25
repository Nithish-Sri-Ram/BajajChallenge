package com.bajajfinserv.health.challenge.model;

import lombok.Data;
import java.util.List;

@Data
public class WebhookRequest {
    private String regNo;
    private List<List<Integer>> outcome;
}