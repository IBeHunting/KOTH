package subside.plugins.koth.captureentities;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.PluginManager;
import subside.plugins.koth.KothPlugin;
import subside.plugins.koth.modules.AbstractModule;
import subside.plugins.koth.modules.ConfigHandler;

@SuppressWarnings("rawtypes")
public class CaptureTypeRegistry extends AbstractModule {
    private @Getter Map<String, Class<? extends Capper>> captureClasses;
    private @Getter Map<String, Class<? extends Capper>> captureTypes;
    private @Getter @Setter Class<? extends Capper> preferedClass;
    
    public CaptureTypeRegistry(KothPlugin plugin){
        super(plugin);
        captureTypes = new HashMap<>();
        captureClasses = new HashMap<>();
    }
    
    @Override
    public void onLoad(){
        captureTypes.clear();
        captureClasses.clear();

        PluginManager pluginManager = plugin.getServer().getPluginManager();
        ConfigHandler.Hooks hooks = plugin.getConfigHandler().getHooks();
        
        // Add the player entity
        registerCaptureClass("capperclass", Capper.class);
        registerCaptureType("player", CappingPlayer.class, true);
    }
    
    @Override
    public void onEnable(){
        // Set the default capture type that is defined in the config
        // The reason we're doing this here instead of the onload, is so other plugins can hook into the plugin
        // without overwriting the prefered class in the config
        if(getCaptureTypeClass(plugin.getConfigHandler().getKoth().getDefaultCaptureType()) != null)
            setPreferedClass(getCaptureTypeClass(plugin.getConfigHandler().getKoth().getDefaultCaptureType()));
    }
    
    /**
     * Use this function to register a capture type class that can actively capture a KoTH.
     * 
     * @param captureTypeIdentifier an unique identifier for this class specifically
     * @param clazz the class object
     */
    public void registerCaptureType(String captureTypeIdentifier, Class<? extends Capper> clazz){
        captureTypes.put(captureTypeIdentifier, clazz);

        // Also register it as a capture class
        registerCaptureClass(captureTypeIdentifier, clazz);
    }
    
    /**
     * This function is a shortcut for registering the capturetype as well as prefering it
     * @param captureTypeIdentifier The unique capturetype identifier
     * @param clazz The class of the capturetype
     * @param isPrefered if the class should be prefered or not
     */
    public void registerCaptureType(String captureTypeIdentifier, Class<? extends Capper> clazz, boolean isPrefered){
        registerCaptureType(captureTypeIdentifier, clazz);
        
        if(isPrefered)
            setPreferedClass(clazz);
    }
    
    /**
     * This class is automatically called when you register a capture type.
     * This is internally also used for the Capper class and the CappingGroup class
     * @param captureClassIdentifier the unique capture class identifier
     * @param clazz the class
     */
    public void registerCaptureClass(String captureClassIdentifier, Class<? extends Capper> clazz){
        captureClasses.put(captureClassIdentifier, clazz);
    }
    
    /**
     * Get a specific capture type class from its unique name
     * @param name the unique identifier of the class
     * @return The class
     */
    public Class<? extends Capper> getCaptureTypeClass(String name){
        return captureTypes.get(name);
    }
    
    /**
     * Get a specific capture class from its unique name
     * @param name the unique identifier of the class
     * @return The class
     */
    public Class<? extends Capper> getCaptureClass(String name){
        return captureClasses.get(name);
    }
    
    /**
     * Get the unique identifier from a specific class
     * @param clazz the class object
     * @return The unique identifier (or null if it doesn't exist)
     */
    public String getIdentifierFromClass(Class<? extends Capper> clazz){
        return captureClasses.entrySet().stream()
                .filter(entry -> clazz.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findAny().orElse(null);
    }
    
    /**
     * Generate an instance of a Capper object from the class unique identifier and object unique identifier
     * @param captureTypeIdentifier the class unique identifier
     * @param objectUniqueId the object unique identifier
     * @return the Capper object (or null if it couldn't be created or if something went wrong)
     */
    public Capper getCapperFromType(String captureTypeIdentifier, String objectUniqueId){
        if(!captureTypes.containsKey(captureTypeIdentifier)){
            return null;
        }
        try {
            Capper capper =  captureTypes.get(captureTypeIdentifier).getDeclaredConstructor(CaptureTypeRegistry.class, String.class).newInstance(this, objectUniqueId);
            if(capper.getObject() == null){
                return null;
            }
            return capper;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate a Capper from a random list of players
     * @param capperClazz the class of the object that should be created
     * @param players a list of players
     * @return the Capper object (or null if it couldn't be created or if something went wrong)
     */
    public Capper getCapper(Class<? extends Capper> capperClazz, List<Player> players){
        try {
            for(Class<? extends Capper> clazz : getCaptureTypes().values()){
                if(!capperClazz.isAssignableFrom(clazz)){
                    continue;
                }
                
                // Create the object
                Capper capper = clazz.getDeclaredConstructor(CaptureTypeRegistry.class, Collection.class).newInstance(this, players);
                if(capper.getObject() == null){
                    return null;
                }
                return capper;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        
        return null;
    }
}
