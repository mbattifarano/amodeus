/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.dispatcher.shared;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.passenger.AVRequest;

/** Middle level class in SharedRoboTaxi functionality, a {@link SharedMenu} is
 * composed of {@link SharedCourse}s which internally have a {@link SharedMealType}s */
public class SharedCourse {

    /** fast access functions */
    public static SharedCourse pickupCourse(AVRequest avRequest) {
        Objects.requireNonNull(avRequest);
        return new SharedCourse(avRequest, avRequest.getFromLink(), avRequest.getId().toString(), SharedMealType.PICKUP);
    }

    public static SharedCourse dropoffCourse(AVRequest avRequest) {
        Objects.requireNonNull(avRequest);
        return new SharedCourse(avRequest, avRequest.getToLink(), avRequest.getId().toString(), SharedMealType.DROPOFF);
    }

    /** @param link the destination of the redirection
     * @param courseId is an unique identifier for an Redirect Course
     * @return */
    public static SharedCourse redirectCourse(Link link, String courseId) {
        // TODO Claudio, Lukas. What would be a good solution for this id? might it be a possibility to at a new AV Request here? new AVRequest()
        return new SharedCourse(STANDARD_REDIRECT_AVREQUEST, link, courseId, SharedMealType.REDIRECT);
    }

    /** class implementation */
    private final String courseID;
    private final Link link;
    private final SharedMealType sharedRoboTaxiMealType;
    private final AVRequest avRequest;
    protected static final AVRequest STANDARD_REDIRECT_AVREQUEST = null;

    /** @param for {@link SharedMealType} PICKUP and DROPOFF the requestID must be the
     *            id of the {@link AVRequest}, otherwise a self-chosen id to distinguish different
     *            {@link SharedMealType} tasks of type REDIRECT
     * @param link
     * @param sharedAVMealType */
    protected SharedCourse(AVRequest avRequest, Link link, String courseId, SharedMealType sharedAVMealType) {
        Objects.requireNonNull(link);
        this.link = link;
        this.avRequest = avRequest;
        Objects.requireNonNull(courseId);
        this.courseID = courseId;
        Objects.requireNonNull(sharedAVMealType);
        this.sharedRoboTaxiMealType = sharedAVMealType;
    }

    public SharedMealType getMealType() {
        return sharedRoboTaxiMealType;
    }

    public String getCourseId() {
        return courseID;
    }

    public Link getLink() {
        return link;
    }

    public AVRequest getAvRequest() {
        return avRequest;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SharedCourse) {
            SharedCourse sharedAVCourse = (SharedCourse) object;
            return sharedAVCourse.getCourseId().equals(courseID) && //
                    sharedAVCourse.getLink().equals(link) && //
                    sharedAVCourse.getMealType().equals(sharedRoboTaxiMealType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseID, sharedRoboTaxiMealType, link);
    }
}
