package org.opennms.horizon.datachoices.service.dto;

import lombok.Data;

import java.util.Date;

@Data
public class CreateSimpleParams {
    private String triggerName;
    private Date startTime;
    private Long repeatTime;
    private int misFireInstruction;
}
