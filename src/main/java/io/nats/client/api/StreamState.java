// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.api;

import io.nats.client.support.JsonUtils;

import java.time.ZonedDateTime;
import java.util.List;

import static io.nats.client.support.ApiConstants.*;

public class StreamState {
    private final long msgs;
    private final long bytes;
    private final long firstSeq;
    private final long lastSeq;
    private final long consumerCount;
    private final long subjectCount;
    private final long deletedCount;
    private final ZonedDateTime firstTime;
    private final ZonedDateTime lastTime;
    private final List<Subject> subjects;
    private final List<Long> deletedStreamSequences;
    private final LostStreamData lostStreamData;

    StreamState(String json) {
        msgs = JsonUtils.readLong(json, MESSAGES_RE, 0);
        bytes = JsonUtils.readLong(json, BYTES_RE, 0);
        firstSeq = JsonUtils.readLong(json, FIRST_SEQ_RE, 0);
        lastSeq = JsonUtils.readLong(json, LAST_SEQ_RE, 0);
        consumerCount = JsonUtils.readLong(json, CONSUMER_COUNT_RE, 0);
        firstTime = JsonUtils.readDate(json, FIRST_TS_RE);
        lastTime = JsonUtils.readDate(json, LAST_TS_RE);
        subjectCount = JsonUtils.readLong(json, NUM_SUBJECTS_RE, 0);
        deletedCount = JsonUtils.readLong(json, NUM_DELETED_RE, 0);
        subjects = Subject.getList(JsonUtils.getJsonObject(SUBJECTS, json));
        deletedStreamSequences = JsonUtils.getLongList(DELETED, json);
        lostStreamData = LostStreamData.optionalInstance(json);
    }

    /**
     * Gets the message count of the stream.
     *
     * @return the message count
     */
    public long getMsgCount() {
        return msgs;
    }

    /**
     * Gets the byte count of the stream.
     *
     * @return the byte count
     */
    public long getByteCount() {
        return bytes;
    }

    /**
     * Gets the first sequence number of the stream.
     *
     * @return a sequence number
     */
    public long getFirstSequence() {
        return firstSeq;
    }

    /**
     * Gets the time stamp of the first message in the stream
     *
     * @return the first time
     */
    public ZonedDateTime getFirstTime() {
        return firstTime;
    }

    /**
     * Gets the last sequence of a message in the stream
     *
     * @return a sequence number
     */
    public long getLastSequence() {
        return lastSeq;
    }

    /**
     * Gets the time stamp of the last message in the stream
     *
     * @return the first time
     */
    public ZonedDateTime getLastTime() {
        return lastTime;
    }

    /**
     * Gets the number of consumers attached to the stream.
     *
     * @return the consumer count
     */
    public long getConsumerCount() {
        return consumerCount;
    }

    /**
     * Gets the count of subjects in the stream.
     *
     * @return the subject count
     */
    public long getSubjectCount() {
        return subjectCount;
    }

    /**
     * Get a list of the Subject objects. May be null if the Stream Info request did not ask for subjects
     * or if there are no subjects.
     * @return the list of subjects
     */
    public List<Subject> getSubjects() {
        return subjects;
    }

    /**
     * Gets the count of deleted messages
     *
     * @return the deleted count
     */
    public long getDeletedCount() {
        return deletedCount;
    }

    /**
     * Get a list of the Deleted objects. May be null if the Stream Info request did not ask for subjects
     * or if there are no subjects.
     * @return the list of subjects
     */
    public List<Long> getDeleted() {
        return deletedStreamSequences;
    }

    /**
     * Get the lost stream data information if available.
     * @return the LostStreamData
     */
    public LostStreamData getLostStreamData() {
        return lostStreamData;
    }

    @Override
    public String toString() {
        return "StreamState{" +
            "msgs=" + msgs +
            ", bytes=" + bytes +
            ", firstSeq=" + firstSeq +
            ", lastSeq=" + lastSeq +
            ", consumerCount=" + consumerCount +
            ", firstTime=" + firstTime +
            ", lastTime=" + lastTime +
            ", subjectCount=" + subjectCount +
            ", subjects=" + subjects +
            ", deletedCount=" + deletedCount +
            ", deleteds=" + deletedStreamSequences +
            ", lostStreamData=" + lostStreamData +
            '}';
    }
}
