/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartloli.kafka.eagle.ipc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.UUID;

import org.apache.thrift.TException;
import org.smartloli.kafka.eagle.util.ConstantUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import kafka.common.OffsetAndMetadata;
import kafka.server.GroupTopicPartition;

/**
 * Implements kafka rpc api.
 * 
 * @author smartloli.
 *
 *         Created by Jan 5, 2017
 * 
 * @see org.smartloli.kafka.eagle.ipc.KafkaOffsetServer
 */
public class KafkaOffsetServerImpl extends KafkaOffsetGetter implements KafkaOffsetServer.Iface {

	/** According to group, topic & partition to get the topic data in Kafka. */
	@Override
	public String query(String group, String topic, int partition) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	/** Get offset in Kafka topic. */
	@Override
	public String getOffset() throws TException {
		JSONArray array = new JSONArray();
		Map<String, Boolean> map = getActiver();
		for (Entry<GroupTopicPartition, OffsetAndMetadata> entry : offsetMap.entrySet()) {
			JSONObject object = new JSONObject();
			object.put("group", entry.getKey().group());
			object.put("topic", entry.getKey().topicPartition().topic());
			object.put("partition", entry.getKey().topicPartition().partition());
			object.put("offset", entry.getValue().offset());
			object.put("timestamp", entry.getValue().timestamp());
			String key = entry.getKey().group() + ConstantUtils.Separator.EIGHT + entry.getKey().topicPartition().topic() + ConstantUtils.Separator.EIGHT + entry.getKey().topicPartition().partition();
			if (map.containsKey(key)) {
				UUID uuid = UUID.randomUUID();
				String threadId = String.format("%s-%d-%s-%d", entry.getKey().group(), System.currentTimeMillis(), (uuid.getMostSignificantBits() + "").substring(0, 8), entry.getKey().topicPartition().partition());
				object.put("owner", threadId);
			} else {
				object.put("owner", "");
			}
			array.add(object);
		}
		return array.toJSONString();
	}

	/** Using SQL to get data from Kafka in topic. */
	@Override
	public String sql(String sql) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	/** Get consumer from Kafka in topic. */
	@Override
	public String getConsumer() throws TException {
		Map<String, Set<String>> map = new HashMap<>();
		for (Entry<GroupTopicPartition, OffsetAndMetadata> entry : offsetMap.entrySet()) {
			String group = entry.getKey().group();
			String topic = entry.getKey().topicPartition().topic();
			if (map.containsKey(group)) {
				Set<String> set = map.get(group);
				set.add(topic);
			} else {
				Set<String> set = new HashSet<>();
				set.add(topic);
				map.put(group, set);
			}
		}
		Map<String, List<String>> map2 = new HashMap<>();
		for (Entry<String, Set<String>> entry : map.entrySet()) {
			List<String> list = new ArrayList<>();
			for (String topic : entry.getValue()) {
				list.add(topic);
			}
			map2.put(entry.getKey(), list);
		}
		return map.toString();
	}

	/** Get activer from Kafka in topic. */
	private Map<String, Boolean> getActiver() throws TException {
		long mill = System.currentTimeMillis();
		Map<String, Boolean> active = new ConcurrentHashMap<>();
		for (Entry<GroupTopicPartition, OffsetAndMetadata> entry : offsetMap.entrySet()) {
			String group = entry.getKey().group();
			String topic = entry.getKey().topicPartition().topic();
			int partition = entry.getKey().topicPartition().partition();
			long timespan = entry.getValue().timestamp();
			String key = group + ConstantUtils.Separator.EIGHT + topic + ConstantUtils.Separator.EIGHT + partition;
			if (active.containsKey(key)) {
				if ((mill - timespan) <= ConstantUtils.Kafka.ACTIVER_INTERVAL) {
					active.put(key, true);
				} else {
					active.put(key, false);
				}
			} else {
				active.put(key, true);
			}
		}
		return active;
	}

	/** Get active consumer from Kafka in topic. */
	@Override
	public String getActiverConsumer() throws TException {
		long mill = System.currentTimeMillis();
		for (Entry<GroupTopicPartition, OffsetAndMetadata> entry : offsetMap.entrySet()) {
			String group = entry.getKey().group();
			String topic = entry.getKey().topicPartition().topic();
			int partition = entry.getKey().topicPartition().partition();
			long timespan = entry.getValue().timestamp();
			String key = group + ConstantUtils.Separator.EIGHT + topic + ConstantUtils.Separator.EIGHT + partition;
			if (activeMap.containsKey(key)) {
				if ((mill - timespan) <= ConstantUtils.Kafka.ACTIVER_INTERVAL) {
					activeMap.put(key, true);
				} else {
					activeMap.put(key, false);
				}
			} else {
				activeMap.put(key, true);
			}
		}

		Map<String, Set<String>> map = new HashMap<>();
		for (Entry<String, Boolean> entry : activeMap.entrySet()) {
			if (entry.getValue()) {
				String[] kk = entry.getKey().split(ConstantUtils.Separator.EIGHT);
				String key = kk[0] + "_" + kk[1];
				String topic = kk[1];
				if (map.containsKey(key)) {
					map.get(key).add(topic);
				} else {
					Set<String> list = new HashSet<>();
					list.add(topic);
					map.put(key, list);
				}
			}
		}

		Map<String, List<String>> map2 = new HashMap<>();
		for (Entry<String, Set<String>> entry : map.entrySet()) {
			List<String> list = new ArrayList<>();
			for (String topic : entry.getValue()) {
				list.add(topic);
			}
			map2.put(entry.getKey(), list);
		}

		return map2.toString();
	}

	/** Get consumer page data from Kafka in topic. */
	@Override
	public String getConsumerPage(String search, int iDisplayStart, int iDisplayLength) throws TException {
		Map<String, Set<String>> map = new HashMap<>();
		int offset = 0;
		for (Entry<GroupTopicPartition, OffsetAndMetadata> entry : offsetMap.entrySet()) {
			String group = entry.getKey().group();
			String topic = entry.getKey().topicPartition().topic();
			if (search.length() > 0 && search.equals(group)) {
				if (map.containsKey(group)) {
					Set<String> set = map.get(group);
					set.add(topic);
				} else {
					Set<String> set = new HashSet<>();
					set.add(topic);
					map.put(group, set);
				}
				break;
			} else if (search.length() == 0) {
				if (offset < (iDisplayLength + iDisplayStart) && offset >= iDisplayStart) {
					if (map.containsKey(group)) {
						Set<String> set = map.get(group);
						set.add(topic);
					} else {
						Set<String> set = new HashSet<>();
						set.add(topic);
						map.put(group, set);
					}
				}
				offset++;
			}
		}
		Map<String, List<String>> map2 = new HashMap<>();
		for (Entry<String, Set<String>> entry : map.entrySet()) {
			List<String> list = new ArrayList<>();
			for (String topic : entry.getValue()) {
				list.add(topic);
			}
			map2.put(entry.getKey(), list);
		}
		return map.toString();
	}

}