using System;
using System.Collections.Generic;
using System.Text;

namespace QLearningProject.MachineLearning
{
    public interface IQLearningProblem
    {
        int NumberOfStates { get; }
        int NumberOfActions { get; }
        double GetReward(int currentState, int action);
        //bool GoalStateIsReached(int currentState);
        int GetState(int L1, int L2, int VStalenessAvg);
    }
}
