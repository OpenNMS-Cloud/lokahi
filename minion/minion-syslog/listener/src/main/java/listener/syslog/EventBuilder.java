/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package listener.syslog;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyAccessorFactory;

import java.net.InetAddress;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * <p>EventBuilder class.</p>
 */
public class EventBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(EventBuilder.class);

    private Event m_event;




    public EventBuilder() {
        m_event = new Event();
    }

    /**
     * <p>Constructor for EventBuilder.</p>
     *
     * @param uei a {@link String} object.
     * @param source a {@link String} object.
     */
    public EventBuilder(final String uei, final String source) {
        this(uei, source, new Date());
    }

    /**
     * <p>Constructor for EventBuilder.</p>
     *
     * @param uei a {@link String} object.
     * @param source a {@link String} object.
     * @param date a {@link Date} object.
     */
    public EventBuilder(final String uei, final String source, final Date date) {
        m_event = new Event();
        setUei(uei);
        setTime(date);
        setSource(source);
        checkForIllegalUei();
    }


    public EventBuilder(final Event event) {
        m_event = event;
        Date now = new Date();
        setTime(now);
        checkForIllegalUei();
    }


    protected void checkForIllegalUei(){
        if(EventConstants.NODE_LABEL_CHANGED_EVENT_UEI.equals(this.m_event.getUei())){
            LOG.warn("The use of EventBuilder is deprecated for UEI="+EventConstants.NODE_LABEL_CHANGED_EVENT_UEI
                    +", use NodeLabelChangedEventBuilder instead");
        }
    }



    public Event getEvent() {


        if (m_event.getCreationTime() == null) {
            // The creation time has been used as the time when the event
            // is stored in the database so update it right before we return
            // the event object.
            m_event.setCreationTime(new Date());
        }
        return m_event;
    }

    public Log getLog() {
        Event event = getEvent();

        Events events = new Events();
        events.setEvent(new Event[]{event});

        Header header = new Header();
        header.setCreated(StringUtils.toStringEfficiently(event.getCreationTime()));

        Log log = new Log();
        log.setHeader(header);
        log.setEvents(events);
        return log;
    }

    public EventBuilder setUei(final String uei) {
        m_event.setUei(uei);
        checkForIllegalUei();
        return this;
    }


    /**
     * <p>setTime</p>
     *
     * @param date a {@link Date} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setTime(final Date date) {
       m_event.setTime(date);
       return this;
    }




    /**
     * <p>setSource</p>
     *
     * @param source a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setSource(final String source) {
        m_event.setSource(source);
        return this;
        
    }


    /**
     * <p>setNodeid</p>
     *
     * @param nodeid a long.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setNodeid(long nodeid) {
        m_event.setNodeid(nodeid);
        return this;
    }

    /**
     * <p>setHost</p>
     *
     * @param hostname a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setHost(final String hostname) {
        m_event.setHost(hostname);
        return this;
    }
    
    /**
     * <p>setInterface</p>
     *
     * @param ipAddress a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setInterface(final InetAddress ipAddress) {
        if (ipAddress != null) m_event.setInterfaceAddress(ipAddress);
        return this;
    }

    public EventBuilder setIfIndex(final int ifIndex) {
        m_event.setIfIndex(ifIndex);
        return this;
    }

    /**
     * <p>setService</p>
     *
     * @param serviceName a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setService(final String serviceName) {
        m_event.setService(serviceName);
        return this;
    }

    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final String val) {
        return addParam(parmName, val, null, null);
    }
    
    public EventBuilder addParam(final String parmName, final String val, final String type, final String encoding) {
        if (parmName != null) {
            Value value = new Value();
            value.setContent(val);
            
            if (type != null) {
                value.setType(type);
            }
            
            if (encoding != null) {
                value.setEncoding(encoding);
            }

            Parm parm = new Parm();
            parm.setParmName(parmName);
            parm.setValue(value);

            addParam(parm);
        }
        
        return this;
    }

    
    public EventBuilder addParam(final Parm parm) {
        m_event.addParm(parm);

        return this;
    }

    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final boolean val) {
        if (parmName != null) {
            final Value value = new Value();
            value.setContent(val ? "true" : "false");

            final Parm parm = new Parm();
            parm.setParmName(parmName);
            parm.setValue(value);

            this.addParam(parm);
        }

        return this;
    }
    
    /**
     * <p>setParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setParam(final String parmName, final String val) {
        if (m_event.getParmCollection().size() < 1) {
            return addParam(parmName, val);
        }

        for(final Parm parm : m_event.getParmCollection()) {
            if (parm.getParmName().equals(parmName)) {
            	final Value value = new Value();
                value.setContent(val);
                parm.setValue(value);
                return this;
            }
        }

        return addParam(parmName, val);
    }

    /**
     * <p>setParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a int.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setParam(final String parmName, final int val) {
        return setParam(parmName, Integer.toString(val));
    }

    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a long.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final double val) {
        return addParam(parmName, Double.toString(val));
    }
    
    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a long.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final long val) {
        return addParam(parmName, Long.toString(val));
    }
    
    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param val a int.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final int val) {
        return addParam(parmName, Integer.toString(val));
    }
    
    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param ch a char.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final char ch) {
        return addParam(parmName, Character.toString(ch));
    }
    
    /**
     * <p>addParam</p>
     *
     * @param parmName a {@link String} object.
     * @param vals a {@link Collection} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder addParam(final String parmName, final Collection<String> vals) {
        final String val = org.springframework.util.StringUtils.collectionToCommaDelimitedString(vals);
        return addParam(parmName, val);
        
    }


    public EventBuilder setAlarmData(final AlarmData alarmData) {
        if (alarmData != null) {
            m_event.setAlarmData(alarmData);
        }
        return this;
    }
    






    /**
     * <p>setSnmpVersion</p>
     *
     * @param version a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setSnmpVersion(final String version) {
    	ensureSnmp();
    	m_event.getSnmp().setVersion(version);
		return this;
	}

	private void ensureSnmp() {
		if (m_event.getSnmp() == null) {
			m_event.setSnmp(new Snmp());
		}
		
	}
	
	public EventBuilder setCommunity(final String community) {
	    ensureSnmp();
	    m_event.getSnmp().setCommunity(community);
	    return this;
	}

	/**
	 * <p>setEnterpriseId</p>
	 *
	 * @param enterprise a {@link String} object.
	 * @return a {@link EventBuilder} object.
	 */
	public EventBuilder setEnterpriseId(final String enterprise) {
		ensureSnmp();
		m_event.getSnmp().setId(enterprise);
		return this;
	}

	public EventBuilder setTrapOID(final String trapOID) {
	    ensureSnmp();;
	    m_event.getSnmp().setTrapOID(trapOID);
	    return this;
    }

	/**
	 * <p>setGeneric</p>
	 *
	 * @param generic a int.
	 * @return a {@link EventBuilder} object.
	 */
	public EventBuilder setGeneric(final int generic) {
		ensureSnmp();
		m_event.getSnmp().setGeneric(generic);
		return this;
	}

	/**
	 * <p>setSpecific</p>
	 *
	 * @param specific a int.
	 * @return a {@link EventBuilder} object.
	 */
	public EventBuilder setSpecific(final int specific) {
		ensureSnmp();
		m_event.getSnmp().setSpecific(specific);
		return this;
	}

	/**
	 * <p>setSnmpHost</p>
	 *
	 * @param snmpHost a {@link String} object.
	 * @return a {@link EventBuilder} object.
	 */
	public EventBuilder setSnmpHost(final String snmpHost) {
		m_event.setSnmphost(snmpHost);
		return this;
		
	}
	
    public EventBuilder setSnmpTimeStamp(final long timeStamp) {
        ensureSnmp();
        m_event.getSnmp().setTimeStamp(timeStamp);
        return this;
    }



    /**
     * <p>setField</p>
     *
     * @param name a {@link String} object.
     * @param val a {@link String} object.
     */
    public void setField(final String name, final String val) {
        if (name.equals("eventparms")) {
            String[] parts = val.split(";");
            for (String part : parts) {
                String[] pair = part.split("=");
                addParam(pair[0], pair[1].replaceFirst("[(]\\w+,\\w+[)]", ""));
            }
        } else {
            final BeanWrapper w = PropertyAccessorFactory.forBeanPropertyAccess(m_event);
            try {
                w.setPropertyValue(name, val);
            } catch (final BeansException e) {
                LOG.warn("Could not set field on event: {}", name, e);
            }
        }
    }
    
    private void ensureLogmsg() {
        if (m_event.getLogmsg() == null) {
            m_event.setLogmsg(new Logmsg());
        }
    }

    /**
     * <p>setLogDest</p>
     *
     * @param dest a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setLogDest(final String dest) {
        ensureLogmsg();
        m_event.getLogmsg().setDest(dest);
        return this;
    }

    /**
     * <p>setLogMessage</p>
     *
     * @param content a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setLogMessage(final String content) {
        ensureLogmsg();
        m_event.getLogmsg().setContent(content);
        return this;
    }

    /**
     * <p>setDescription</p>
     *
     * @param descr a {@link String} object.
     * @return a {@link EventBuilder} object.
     */
    public EventBuilder setDescription(final String descr) {
        m_event.setDescr(descr);
        return this;
    }

    /**
     * <p>setParms</p>
     *
     * @param parms a list of parameters.
     * @return the event builder
     */
    public EventBuilder setParms(final List<Parm> parms) {
        m_event.setParmCollection(parms);
        return this;
    }

	public EventBuilder setUuid(final String uuid) {
		m_event.setUuid(uuid);
		return this;
	}

	public EventBuilder setDistPoller(final String distPoller) {
		m_event.setDistPoller(distPoller);
		return this;
	}

	public EventBuilder setMasterStation(final String masterStation) {
		m_event.setMasterStation(masterStation);
		return this;
	}



}
