package protocols.dht.ChordTimers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class FixFingersTimer extends ProtoTimer {

    public static final short TIMER_ID = 8277;

    public FixFingersTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
