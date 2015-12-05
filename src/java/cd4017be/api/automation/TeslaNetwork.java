/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cd4017be.api.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author CD4017BE
 */
public class TeslaNetwork 
{
    public static TeslaNetwork instance = new TeslaNetwork();
    
    private Map<Short, List<ITeslaTransmitter>> network = new HashMap();
    
    public void register(ITeslaTransmitter transmitter)
    {
        short id = transmitter.getFrequency();
        List<ITeslaTransmitter> list = network.get(id);
        if (list == null)
        {
            list = new ArrayList();
            network.put(id, list);
        }
        if (!list.contains(transmitter)) list.add(transmitter);
    }
    
    public void remove(ITeslaTransmitter transmitter)
    {
        short id = transmitter.getFrequency();
        List<ITeslaTransmitter> list = network.get(id);
        if (list == null) return;
        list.remove(transmitter);
        if (list.isEmpty()) network.remove(id);
    }
    
    public void unloadAll()
    {
        network.clear();
    }
    
    public void repairMap()
    {
        Map.Entry<Short, List<ITeslaTransmitter>>[] objects = (Map.Entry<Short, List<ITeslaTransmitter>>[])network.entrySet().toArray();
        network.clear();
        for (Map.Entry<Short, List<ITeslaTransmitter>> entry : objects)
        {
            Iterator<ITeslaTransmitter> iterator = entry.getValue().iterator();
            while(iterator.hasNext())
            {
            	ITeslaTransmitter te = iterator.next();
                if (te.checkAlive())
                {
                    register(te);
                }
            }
        }
    }
    
    public void transmittEnergy(ITeslaTransmitter transmitter)
    {
        short id = (short)transmitter.getFrequency();
        if (id <= 0) return;
        List<ITeslaTransmitter> list = network.get(id);
        if (list == null || !list.contains(transmitter))
        {
            register(transmitter);
            return;
        }
        Iterator<ITeslaTransmitter> iterator = list.iterator();
        List<ITeslaTransmitter> remove = new ArrayList();
        while(iterator.hasNext())
        {
        	ITeslaTransmitter te = iterator.next();
            if (!te.checkAlive() || te.getFrequency() != id)
            {
                remove.add(te);
                continue;
            }
            double d = (transmitter.getSqDistance(te) + te.getSqDistance(transmitter));
            if (d < 1D) continue;
            transmitter.addEnergy(-te.addEnergy(transmitter.getPower(d, te.getVoltage())));
        }
        if (!remove.isEmpty()) list.removeAll(remove); 
    }
}
