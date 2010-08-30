package de.uniluebeck.itm.tr.logcontroller;

import javax.persistence.*;

/**
 * jpa-entity for binarymessage
 */
@Entity
@Table(name = "WsnMessages")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "messagetype",
        discriminatorType = DiscriminatorType.STRING
)
@DiscriminatorValue("Binary")
public class BinaryMessage extends AbstractMessage {
    @Lob
    public byte[] binaryData;
    public Byte binaryType;
}
