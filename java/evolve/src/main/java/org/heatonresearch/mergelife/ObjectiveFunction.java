package org.heatonresearch.mergelife;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ObjectiveFunction {

    public static class ObjectiveFunctionStat {
        private final String stat;
        private final double min;
        private final double max;
        private final double weight;
        private final double minWeight;
        private final double maxWeight;

        public ObjectiveFunctionStat(String stat, double min, double max, double weight, double minWeight, double maxWeight) {
            this.stat = stat;
            this.min = min;
            this.max = max;
            this.weight = weight;
            this.minWeight = minWeight;
            this.maxWeight = maxWeight;
        }

        public String getStat() {
            return stat;
        }

        public double calculate(CalculateObjectiveStats stats) {
            double range = this.max - this.min;
            double ideal = range / 2;
            if( !stats.getCurrentStats().containsKey(this.stat)) {
                throw new MergeLifeException("Unknown objective stat: " + this.stat);
            }
            double actual = stats.getCurrentStats().get(this.stat);

            if(actual < this.min) {
                // too small
                return this.minWeight;
            }
            else if(actual > this.max) {
                // too big
                return this.maxWeight;
            } else {
                double adjust = ((range / 2) - Math.abs(actual - ideal)) / (range / 2);
                adjust *= this.weight;
                return adjust;
            }
        }
    }

    private final List<ObjectiveFunctionStat> stats = new ArrayList<>();

    public ObjectiveFunction(String filename) throws IOException {
        byte[] mapData = Files.readAllBytes(Paths.get(filename));
        //Map<String,String> myMap = new HashMap<String, String>();
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<Object> list = objectMapper.readValue(mapData, ArrayList.class);
        for(Object obj: list) {
            Map map = (Map)obj;
            String stat = (String)map.get("stat");
            double min = Double.parseDouble(map.get("min").toString());
            double max = Double.parseDouble(map.get("max").toString());
            double weight = Double.parseDouble(map.get("weight").toString());
            double minWeight = Double.parseDouble(map.get("min_weight").toString());
            double maxWeight = Double.parseDouble(map.get("max_weight").toString());
            this.stats.add(new ObjectiveFunctionStat(stat,min,max,weight,minWeight,maxWeight));
        }
    }

    public List<ObjectiveFunctionStat> getStats() {
        return stats;
    }

    public double calculateObjective(String ruleText) {
        MergeLifeGrid grid = new MergeLifeGrid(100, 100);
        MergeLifeRule rule = new MergeLifeRule(ruleText);
        grid.randomize(0,new Random());

        CalculateObjectiveStats calcStats = new CalculateObjectiveStats(grid);

        while(!calcStats.hasStabilized()) {
            grid.step(rule);
            System.out.println(calcStats.track());
        }

        double score = 0;
        for(ObjectiveFunctionStat stat: this.stats) {
            score += stat.calculate(calcStats);
        }

        return score;
    }

    public static void main(String[] args) {
        try {
            ObjectiveFunction obj = new ObjectiveFunction("D:\\Users\\jheaton\\projects\\mergelife\\java\\evolve\\paperObjective.json");
            System.out.println(obj.calculateObjective("E542-5F79-9341-F31E-6C6B-7F08-8773-7068"));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}