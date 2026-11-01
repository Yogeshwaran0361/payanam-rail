package com.payanam.rail.domain.engine;

/**
 * Callback from the domain to the outside world (Observer pattern). The domain does not know about
 * notifications or databases; the Spring layer plugs in a listener. Previews use a no-op listener.
 */
public interface StationListener {

    /** A listener that ignores everything. */
    StationListener NONE = notice -> { };

    /** Called after a waiting passenger was promoted into a freed seat. */
    void onPromotion(PromotionNotice notice);

    /** Called for each active PNR on a train whose platform, timing or arrival changed. */
    default void onTrainUpdate(TrainUpdateNotice notice) {
    }
}
