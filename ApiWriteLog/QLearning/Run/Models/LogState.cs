using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.Run.Models
{
    /// <summary>
    /// State, action trên bảng reward hoặc q table
    /// </summary>
    public class LogState
    {
        public int l1 { get; set; }
        public int l2 { get; set; }
        public int VStalenessAvg { get; set; }
        public int action { get; set; }

        public override string ToString()
        {
            return "{" + $"l1:{l1}, l2:{l2}, VStalenessAvg:{VStalenessAvg}, action:{action}" + "}";
        }
    }
}
