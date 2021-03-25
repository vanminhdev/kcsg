using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.Run.Models
{
    /// <summary>
    /// Trạng thái và hành động cuối cùng
    /// </summary>
    public class LogState
    {
        public int l1 { get; set; }
        public int l2 { get; set; }
        public int NOE { get; set; }
        public int action { get; set; }

        public override string ToString()
        {
            return "{" + $"l1:{l1}, l2:{l2}, NOE:{NOE}, action:{action}" + "}";
        }
    }
}
