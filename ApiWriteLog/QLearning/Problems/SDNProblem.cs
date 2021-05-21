using QLearningProject.MachineLearning;
using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.Problems
{
    class SDNProblem : IQLearningProblem
    {
        public Dictionary<int, Tuple<int, int, int>> DicState = new Dictionary<int, Tuple<int, int, int>>();
        public double[][] rewards { get; set; }
        public int NumberOfStates => 301 * 301 * 101;
        public int NumberOfActions => 6;

        /// <summary>
        /// Problem chứa danh sách state, bảng reward
        /// old reward là đầu vào để tính ra reward mới
        /// </summary>
        /// <param name="oldRewards"></param>
        public SDNProblem(double[][] oldRewards)
        {
            if (oldRewards == null)
            {
                rewards = new double[NumberOfStates + 1][];
                for (int i = 0; i < NumberOfStates + 1; i++)
                {
                    rewards[i] = new double[] { 0, 0, 0, 0, 0, 0 };
                }
            }
            else
            {
                rewards = oldRewards;
            }
        }

        /// <summary>
        /// với mỗi L1 L2 NOE là 1 action tương ứng
        /// </summary>
        /// <param name="L1"></param>
        /// <param name="L2"></param>
        /// <param name="NOE"></param>
        /// <returns></returns>
        public int GetState(int L1, int L2, int NOE)
        {
            return NOE + L2 * 100 + L1 * 30000;
            //return NOE + L2 * 100 + L1 * 10000;
            //return svalue[L1,L2,NOE];
        }

        /// <summary>
        /// Lấy ra reward tại state và action tương ứng
        /// </summary>
        /// <param name="currentState"></param>
        /// <param name="action"></param>
        /// <returns></returns>
        public double GetReward(int currentState, int action)
        {
            return rewards[currentState][action];
        }

        public string ShowReward()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < NumberOfStates; i++)
            {
                for (int j = 0; j < NumberOfActions; j++)
                {
                    if (rewards[i][j] != 0)
                    {
                        sb.Append($"{i}: {rewards[i][0]} {rewards[i][1]} {rewards[i][2]} {rewards[i][3]} {rewards[i][4]} {rewards[i][5]}\n");
                        break;
                    }
                }
            }
            return sb.ToString();
        }
    }
}
