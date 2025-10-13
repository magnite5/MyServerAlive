package dev.magnoix.msa.data;
@Deprecated
public class Statistic {

    public enum StatisticType {
        KILLS("statistic.kills"),
        EVENT_WINS("statistic.eventwins"),
        DEATHS("statistic.deaths"),
        BLOCKS_BROKEN("statistic.blocks_broken");

        private final String path;
        
        StatisticType(String path) { 
            this.path = path; 
        }
        
        public String getPath() {
            return path;
        }
    }
}
