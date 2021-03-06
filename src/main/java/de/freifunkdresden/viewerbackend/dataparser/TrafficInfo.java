/*
 * The MIT License
 *
 * Copyright 2020 Niklas Merkelt.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.freifunkdresden.viewerbackend.dataparser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;

public class TrafficInfo {

    private final Map<Interface, Long> traffic_in = new EnumMap<>(Interface.class);
    private final Map<Interface, Long> traffic_out = new EnumMap<>(Interface.class);

    public TrafficInfo() {
    }

    public void readValues(JsonObject stats) {
        boolean from_to = false;
        for (Interface out : Interface.values()) {
            for (Interface in : Interface.values()) {
                String name = String.format("traffic_%s_%s", out.name().toLowerCase(), in.name().toLowerCase());
                JsonElement j = stats.get(name);
                if (j != null) {
                    from_to = true;
                    traffic_out.put(out, getOutput(out) + j.getAsLong());
                    traffic_in.put(in, getInput(in) + j.getAsLong());
                }
            }
        }
        if (!from_to) {
            for (Interface i : Interface.values()) {
                String name = String.format("traffic_%s", i.name().toLowerCase());
                JsonElement j = stats.get(name);
                if (j != null) {
                    String[] t = j.getAsString().split(",");
                    if (t.length == 2) {
                        traffic_in.put(i, Long.parseLong(t[0]));
                        traffic_out.put(i, Long.parseLong(t[1]));
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        return traffic_in.isEmpty() && traffic_out.isEmpty();
    }

    public boolean hasInterface(Interface i) {
        return traffic_in.containsKey(i) || traffic_out.containsKey(i);
    }

    public long getInput(Interface i) {
        return traffic_in.getOrDefault(i, 0L);
    }

    public long getOutput(Interface i) {
        return traffic_out.getOrDefault(i, 0L);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Interface i : Interface.values()) {
            if (hasInterface(i)) {
                sb.append(String.format("%s: %d<>%d%n", i.name().toLowerCase(), getInput(i), getOutput(i)));
            }
        }
        return sb.toString();
    }

    public enum Interface {
        LAN,
        WAN,
        ADHOC,
        AP,
        OVPN,
        GWT,
        PRIVNET,
        TBB_FASTD,
        TBB_WG,
        MESH_LAN,
        MESH_WAN,
    }
}
