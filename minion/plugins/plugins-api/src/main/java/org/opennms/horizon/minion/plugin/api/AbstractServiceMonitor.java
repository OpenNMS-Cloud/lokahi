

package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import org.opennms.taskset.contract.Resilience;
import org.opennms.taskset.contract.Resilience.Builder;

public abstract class AbstractServiceMonitor implements ServiceMonitor {

    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public AbstractServiceMonitor() {
        Descriptor resilienceDescriptor = Resilience.getDescriptor();

        retriesFieldDescriptor = resilienceDescriptor.findFieldByNumber(Resilience.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor = resilienceDescriptor.findFieldByNumber(Resilience.TIMEOUT_FIELD_NUMBER);
    }

    @Override
    public String getEffectiveLocation(String location) {
        return location;
    }

    public static Object getKeyedObject(final Map<String, Object> parameterMap, final String key, final Object defaultValue) {
        if (key == null) return defaultValue;

        final Object value = parameterMap.get(key);
        if (value == null) return defaultValue;

        return value;
    }

    protected Resilience populateResilience(Resilience declared, int retries, long timeout) {
        Builder resilienceBuilder = Resilience.newBuilder(declared);

        if (!declared.hasField(retriesFieldDescriptor)) {
            resilienceBuilder.setRetries(retries);
        }
        if (!declared.hasField(timeoutFieldDescriptor)) {
            resilienceBuilder.setTimeout(timeout);
        }

        return resilienceBuilder.build();
    }

    protected Resilience cascadedResilience(Resilience lowerPriority, Resilience higherPriority) {
        Builder resilienceBuilder = Resilience.newBuilder(lowerPriority);

        if (higherPriority.hasField(retriesFieldDescriptor)) {
            resilienceBuilder.setRetries(higherPriority.getRetries());
        }
        if (higherPriority.hasField(timeoutFieldDescriptor)) {
            resilienceBuilder.setTimeout(higherPriority.getTimeout());
        }

        return resilienceBuilder.build();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getKeyedInstance(final Map<String, Object> parameterMap, final String key, final Supplier<T> defaultValue) {
        if (key == null) return defaultValue.get();

        final Object value = parameterMap.get(key);
        if (value == null) return defaultValue.get();

        return (T)value;
    }

    public static Boolean getKeyedBoolean(final Map<String, Object> parameterMap, final String key, final Boolean defaultValue) {
        final Object value = getKeyedObject(parameterMap, key, defaultValue);
        if (value == null) return defaultValue;

        if (value instanceof String) {
            return "true".equalsIgnoreCase((String)value) ? Boolean.TRUE : Boolean.FALSE;
        } else if (value instanceof Boolean) {
            return (Boolean)value;
        }

        return defaultValue;
    }

    public static String getKeyedString(final Map<String, Object> parameterMap, final String key, final String defaultValue) {
        final Object value = getKeyedObject(parameterMap, key, defaultValue);
        if (value == null) return defaultValue;

        if (value instanceof String) {
            return (String)value;
        }

        return value.toString();
    }

    public static Integer getKeyedInteger(final Map<String, Object> parameterMap, final String key, final Integer defaultValue) {
        final Object value = getKeyedObject(parameterMap, key, defaultValue);
        if (value == null) return defaultValue;

        if (value instanceof String) {
            try {
                return Integer.valueOf((String)value);
            } catch (final NumberFormatException e) {
                return defaultValue;
            }
        } else if (value instanceof Integer) {
            return (Integer)value;
        } else if (value instanceof Number) {
            return Integer.valueOf(((Number)value).intValue());
        }

        return defaultValue;
    }

    public static Long getKeyedLong(final Map<String, Object> parameterMap, final String key, final Long defaultValue) {
        final Object value = getKeyedObject(parameterMap, key, defaultValue);
        if (value == null) return defaultValue;

        if (value instanceof String) {
            try {
                return Long.valueOf((String)value);
            } catch (final NumberFormatException e) {
                return defaultValue;
            }
        } else if (value instanceof Long) {
            return (Long)value;
        } else if (value instanceof Number) {
            return Long.valueOf(((Number)value).longValue());
        }

        return defaultValue;
    }

    public static Properties getServiceProperties(final MonitoredService svc) {
        final InetAddress addr = InetAddressUtils.addr(svc.getIpAddr());
        final boolean requireBrackets = addr != null && addr instanceof Inet6Address && !svc.getIpAddr().startsWith("[");
        final Properties properties = new Properties();
        properties.put("ipaddr", requireBrackets ? "[" + svc.getIpAddr() + "]" : svc.getIpAddr());
        properties.put("nodeid", svc.getNodeId());
        properties.put("nodelabel", svc.getNodeLabel());
        properties.put("svcname", svc.getSvcName());
        return properties;
    }
}
