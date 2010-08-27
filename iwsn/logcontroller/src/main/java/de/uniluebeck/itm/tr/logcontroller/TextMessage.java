package de.uniluebeck.itm.tr.logcontroller;

import javax.persistence.*;

/**
 * jpa-entity for TextMessage
 */
@Entity
@Table(name = "WsnMessages")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "messagetype",
        discriminatorType = DiscriminatorType.STRING
)
@DiscriminatorValue("Text")
public class TextMessage extends AbstractMessage {
    public String message;
    public String messageLevel;
}
