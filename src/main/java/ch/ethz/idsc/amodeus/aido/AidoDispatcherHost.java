/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.aido;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.amodeus.dispatcher.core.RebalancingDispatcher;
import ch.ethz.idsc.amodeus.dispatcher.core.UnitCapRoboTaxi;
import ch.ethz.idsc.amodeus.matsim.SafeConfig;
import ch.ethz.idsc.amodeus.net.FastLinkLookup;
import ch.ethz.idsc.amodeus.net.MatsimStaticDatabase;
import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.config.AVDispatcherConfig;
import ch.ethz.matsim.av.dispatcher.AVDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.passenger.AVRequest;
import ch.ethz.matsim.av.plcpc.ParallelLeastCostPathCalculator;

public class AidoDispatcherHost extends RebalancingDispatcher {

    private final Map<Integer, UnitCapRoboTaxi> idRoboTaxiMap = new HashMap<>();
    private final Map<Integer, AVRequest> idRequestMap = new HashMap<>();
    private final FastLinkLookup fastLinkLookup;
    private final StringSocket clientSocket;
    private final int dispatchPeriod;

    protected AidoDispatcherHost(Network network, Config config, AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager, //
            StringSocket clientSocket) {
        super(config, avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        this.clientSocket = Objects.requireNonNull(clientSocket);
        this.fastLinkLookup = new FastLinkLookup(network, MatsimStaticDatabase.INSTANCE);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        this.dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 30);

    }

    @Override
    protected void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {

            if (getRoboTaxis().size() > 0 && idRoboTaxiMap.isEmpty()) {
                getRoboTaxis().forEach(//
                        s -> idRoboTaxiMap.put(MatsimStaticDatabase.INSTANCE.getVehicleIndex(s), s));
            }

            try {
                getAVRequests().forEach(//
                        r -> idRequestMap.put(MatsimStaticDatabase.INSTANCE.getRequestIndex(r), r));

                Tensor status = Tensors.of(RealScalar.of((long) now), //
                        AidoRoboTaxiCompiler.compile(getRoboTaxis()), //
                        AidoRequestCompiler.compile(getAVRequests()), //
                        AidoScoreCompiler.compile(round_now, getRoboTaxis(), getAVRequests()));
                clientSocket.writeln(status);

                String fromClient = null;

                fromClient = clientSocket.readLine();

                Tensor commands = Tensors.fromString(fromClient);
                // TODO consistency checks
                Tensor pickups = commands.get(0);
                for (Tensor pickup : pickups) {
                    UnitCapRoboTaxi roboTaxi = idRoboTaxiMap.get(pickup.Get(0).number().intValue());
                    AVRequest avRequest = idRequestMap.get(pickup.Get(1).number().intValue());
                    setRoboTaxiPickup(roboTaxi, avRequest);
                }

                Tensor rebalances = commands.get(1);
                for (Tensor rebalance : rebalances) {
                    UnitCapRoboTaxi roboTaxi = idRoboTaxiMap.get(rebalance.Get(0).number().intValue());
                    Link link = fastLinkLookup.getLinkFromWGS84(TensorCoords.toCoord(rebalance.get(1)));
                    setRoboTaxiRebalance(roboTaxi, link);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        @Named(AVModule.AV_MODE)
        private Network network;

        @Inject
        private Config config;

        public static StringSocket stringSocket; // TODO

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig avconfig) {
            return new AidoDispatcherHost( //
                    network, config, avconfig, travelTime, router, eventsManager, stringSocket);
        }
    }

}
