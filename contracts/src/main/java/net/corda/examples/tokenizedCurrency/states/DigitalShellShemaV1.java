package net.corda.examples.tokenizedCurrency.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class DigitalShellShemaV1 extends MappedSchema {
    public DigitalShellShemaV1() {
        super(DigitalShellShema.class, 1, ImmutableList.of(AddressState.class));
    }


}

