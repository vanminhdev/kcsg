using QLearningProject.MachineLearning;
using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.Run.Models
{
    public class RWValue
    {
        public int R { get; set; }
        public int W { get; set; }
        public int Action { get; set; }
        public double[][] rewards { get; set; }
        public double[][] qTable { get; set; }

        public int t { get; set; }
        public Dictionary<StateAndAction, int> nPull { get; set; }
    }
}
