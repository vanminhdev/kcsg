using QLearningProject.MachineLearning;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.Problems
{
    class SDNProblem : IQLearningProblem
    {
        public Dictionary<int, Tuple<int, int, int>> DicState = new Dictionary<int, Tuple<int, int, int>>();
        public int[,,] svalue { get; set; } = new int[100, 100, 100];
        public double[][] rewards { get; set; }
        public int NumberOfStates => svalue.Length;
        public int NumberOfActions => 6;

        private int numSuccessForAction;
        private int numRequestForAction;

        private int l1;
        private int l2;
        private int NOE;

        public SDNProblem(int numSuccessForAction, int numRequestForAction,
            int l1, int l2, int NOE, double[][] oldRewards)
        {
            int val = 0;
            for (int i = 0; i < 100; i++)
            {
                for (int j = 0; j < 100; j++)
                {
                    for (int k = 0; k < 100; k++)
                    {
                        svalue[i, j, k] = val;
                        DicState.Add(val, new Tuple<int, int, int>(i, j, k));
                        val++;
                    }
                }
            }

            this.numSuccessForAction = numSuccessForAction;
            this.numRequestForAction = numRequestForAction;

            this.l1 = l1;
            this.l2 = l2;
            this.NOE = NOE;

            if (oldRewards == null)
            {
                rewards = new double[NumberOfStates][];
                for (int i = 0; i < NumberOfStates; i++)
                {
                    rewards[i] = new double[] { 0, 0, 0, 0, 0, 0 };
                }
            }
            else
            {
                rewards = oldRewards;
            }
        }

        public int GetState(int L1, int L2, int NOE)
        {
            return svalue[L1,L2,NOE];
        }

        public Tuple<int, int, int> GetSByValue(int value)
        {
            if (DicState.ContainsKey(value))
            {
                return DicState[value];
            }
            return null;
        }

        public double GetReward(int currentState, int action)
        {
            return rewards[currentState][action];
        }

        public int[] GetValidActions(int currentState)
        {
            List<int> validActions = new List<int>();
            var div = numSuccessForAction / (double)numRequestForAction;
            if (div <= 0.5)
            {
                validActions.Add(0); //w + 2
                validActions.Add(1); //r + 2
            }
            else if (div > 0.5 && div < 1)
            {
                validActions.Add(2); //w + 1
                validActions.Add(3); //r + 1
            }
            else //== 1
            {
                validActions.Add(4); //w - 1
                validActions.Add(5); //r - 1
            }
            return validActions.ToArray();
        }

        public string ShowReward()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < NumberOfStates; i++)
            {
                for (int j = 0; j < NumberOfActions; j++)
                {
                    if (rewards[i][j] > 0)
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
