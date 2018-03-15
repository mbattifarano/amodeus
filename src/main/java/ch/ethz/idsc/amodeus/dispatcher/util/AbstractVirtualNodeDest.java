/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.dispatcher.util;

import java.util.List;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNode;

public interface AbstractVirtualNodeDest {
    List<Link> selectLinkSet(VirtualNode<Link> virtualNode, int size);
}
