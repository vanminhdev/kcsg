using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.MachineLearning.Models
{
    /// <summary>
    /// Lưu vết
    /// </summary>
    public class QLearningStats
    {
        public int InitialState { get; set; }
        public int EndState { get; set; }
        /// <summary>
        /// Trải qua bao nhiêu bước
        /// </summary>
        public int Steps { get; set; }
        /// <summary>
        /// Lưu lại mỗi bước đi qua đâu
        /// </summary>
        public int[] Actions { get; set; }

        public override string ToString()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine($"Agent needed {Steps} steps to find the solution");
            sb.AppendLine($"Agent Initial State: {InitialState}");
            foreach (var action in Actions)
                sb.AppendLine($"Action: {action}");
            sb.AppendLine($"Agent arrived at the goal state: {EndState}");
            return sb.ToString();
        }
    }
}
