using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.MachineLearning
{
    public interface IQLearningProblem
    {
        int NumberOfStates { get; }
        int NumberOfActions { get; }
        /// <summary>
        /// tìm ra những action có thể đi được từ state hiện tại
        /// </summary>
        /// <param name="currentState">state hiện tại</param>
        /// <returns></returns>
        int[] GetValidActions(int currentState);
        double GetReward(int currentState, int action);
        //bool GoalStateIsReached(int currentState);
        int GetState(int L1, int L2, int NOE);
    }
}
