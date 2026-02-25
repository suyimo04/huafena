package com.pollen.management.entity;

import com.pollen.management.entity.enums.FieldType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questionnaire_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    private String label;

    private String groupName;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(columnDefinition = "TEXT")
    private String validationRules;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(columnDefinition = "TEXT")
    private String conditionalLogic;
}
