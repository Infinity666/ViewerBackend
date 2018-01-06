/*
 * The MIT License
 *
 * Copyright 2017 Niklas Merkelt.
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
package de.freifunk_dresden.hopglass;

import com.google.gson.JsonObject;
import de.freifunk_dresden.hopglass.Link.LinkType;
import de.freifunk_dresden.hopglass.Node.NodeType;
import java.util.HashMap;

public class DataParser {

    private final JsonObject data;
    private final int version;

    public DataParser(JsonObject data, int version) {
        this.data = data;
        this.version = version;
    }
    
    public int getNodeId() {
        return data.get("common").getAsJsonObject().get("node").getAsInt();
    }

    public String getCommunity() {
        String com = data.get("common").getAsJsonObject().get("city").getAsString();
        if (com.equals("Meissen")) {
            return "Meißen";
        }
        return com;
    }

    public NodeType getRole() {
        if (version >= 13) {
            switch (data.get("system").getAsJsonObject().get("node_type").getAsString()) {
                case "node":
                    return NodeType.STANDARD;
                //@TODO: Include other node types
            }
        }
        return NodeType.STANDARD;
    }

    public String getModel() {
        return data.get("system").getAsJsonObject().get("model").getAsString();
    }

    public String getFirmwareVersion() {
        return data.get("firmware").getAsJsonObject().get("version").getAsString();
    }

    public String getFirmwareBase() {
        JsonObject firmware = data.get("firmware").getAsJsonObject();
        String DISTRIB_ID = firmware.get("DISTRIB_ID").getAsString();
        String DISTRIB_RELEASE = firmware.get("DISTRIB_RELEASE").getAsString();
        String DISTRIB_REVISION = firmware.get("DISTRIB_REVISION").getAsString();
        return DISTRIB_ID + " " + DISTRIB_RELEASE + " " + DISTRIB_REVISION;
    }

    public String getGatewayIp() {
        return data.get("bmxd").getAsJsonObject().get("gateways").getAsJsonObject().get("selected").getAsString();
    }

    public float getUptime() {
        String jsonUptime = data.get("system").getAsJsonObject().get("uptime").getAsString();
        if (version < 10 && jsonUptime.contains(":")) {
            String[] uptime = jsonUptime.split(" ");
            short days = Short.parseShort(uptime[3]);
            int min;
            String minutes = uptime[5].replace(",", "");
            String time = uptime[6].replace(",", "");
            if (minutes.isEmpty()) {
                if (time.contains(":")) {
                    min = Integer.parseInt(time.split(":")[0]) * 60 + Integer.parseInt(time.split(":")[1]);
                } else {
                    min = Integer.parseInt(time);
                }
            } else {
                if (minutes.contains(":")) {
                    min = Integer.parseInt(minutes.split(":")[0]) * 60 + Integer.parseInt(minutes.split(":")[1]);
                } else {
                    min = Integer.parseInt(minutes);
                }
            }
            return min * 60 + days * 86400;
            //Ab v10
        } else {
            return Float.parseFloat(jsonUptime.split(" ")[0]);
        }
    }

    public double getMemoryUsage() {
        double memTotal = Integer.parseInt(data.get("statistic").getAsJsonObject().get("meminfo_MemTotal").getAsString().split(" ")[0]);
        double memFree = Integer.parseInt(data.get("statistic").getAsJsonObject().get("meminfo_MemFree").getAsString().split(" ")[0]);
        return (memTotal - memFree) / memTotal;
    }

    public float getLoadAvg() {
        return Float.parseFloat(data.get("statistic").getAsJsonObject().get("cpu_load").getAsString().split(" ")[0]);
    }

    public short getClients() {
        return data.get("statistic").getAsJsonObject().get("accepted_user_count").getAsShort();
    }

    public HashMap<Integer, Link> getLinkMap() {
        HashMap<Integer, Link> linkmap = new HashMap<>();
        if (version <= 10) {
            data.get("bmxd").getAsJsonObject().get("routing_tables").getAsJsonObject().get("route").getAsJsonObject().get("link").getAsJsonArray().forEach((link) -> {
                JsonObject l = link.getAsJsonObject();
                String[] split = l.get("target").getAsString().split("\\.");
                int targetId = (Integer.parseInt(split[2]) * 255) + (Integer.parseInt(split[3]) - 1);
                LinkType linkType = Link.getLinkType(l.get("interface").getAsString());
                linkmap.put(targetId, new Link(linkType, DataGen.getNode(targetId), DataGen.getNode(getNodeId())));
            });
        }
        if (version == 10) {
            if (data.get("bmxd").getAsJsonObject().has("links")) {
                data.get("bmxd").getAsJsonObject().get("links").getAsJsonArray().forEach((link) -> {
                    JsonObject l = link.getAsJsonObject();
                    Link lnk = linkmap.get(Integer.parseInt(l.get("node").getAsString()));
                    if (lnk != null) {
                        lnk.setSourceTq(Byte.parseByte(l.get("tq").getAsString()));
                    }
                });
            }
        } else if (version >= 11) {
            data.get("bmxd").getAsJsonObject().get("links").getAsJsonArray().forEach((link) -> {
                JsonObject l = link.getAsJsonObject();
                int targetId = l.get("node").getAsInt();
                LinkType linkType = Link.getLinkType(l.get("interface").getAsString());
                linkmap.put(targetId, new Link(linkType, Byte.parseByte(l.get("tq").getAsString()), DataGen.getNode(targetId), DataGen.getNode(getNodeId())));
            });
        }
        return linkmap;
    }

    public String getName() {
        return data.get("contact").getAsJsonObject().get("name").getAsString();
    }

    public String getEMail() {
        return data.get("contact").getAsJsonObject().get("email").getAsString();
    }
}
