package com.dizsun.timechain.interfaces;

import com.dizsun.timechain.component.Peer;

public interface ICheckDelay {
    void checkDelay(Peer peer, double delay);
}
