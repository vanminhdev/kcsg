using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace QLearningProject.MachineLearning
{
    public class StateAndAction
    {
        public int State { get; set; }
        public int Action { get; set; }

        public override bool Equals(object obj)
        {
            if(obj.GetType() != typeof(StateAndAction))
            {
                return false;
            }

            var sa = obj as StateAndAction;
            if (State == sa.State && Action == sa.Action)
            {
                return true;
            }
            return false;
        }

        public override int GetHashCode()
        {
            return (State.ToString() + Action.ToString()).GetHashCode();
        }
    }

    public class QLearning
    {
        private Random _random = new Random();
        private double _gamma;
        private double _epsilon;
        private double _alpha;
        private double _c;

        private Dictionary<StateAndAction, int> _nPull = new Dictionary<StateAndAction, int>();

        public int T { get; set; }

        public double Gamma { get => _gamma; }
        public double Epsilon { get => _epsilon; }
        public double Alpha { get => _alpha; }

        private double[][] _qTable;
        public double[][] QTable { get => _qTable; }

        private IQLearningProblem _qLearningProblem;
        private LogState[] _logState;

        private int numSuccessForAction;
        private int numRequestForAction;

        public QLearning(double gamma, double epsilon, double alpha, IQLearningProblem qLearningProblem,
            int numSuccessForAction, int numRequestForAction,
            double[][] oldQTable, LogState[] logState)
        {
            _qLearningProblem = qLearningProblem;
            if (oldQTable == null)
            {
                _qTable = new double[_qLearningProblem.NumberOfStates][];
                for (int i = 0; i < _qLearningProblem.NumberOfStates; i++)
                {
                    _qTable[i] = new double[] { 0, 0, 0, 0, 0, 0 };
                }
            }
            else
            {
                _qTable = oldQTable;
            }
            _gamma = gamma;
            _epsilon = epsilon;
            _alpha = alpha;
            _logState = logState;

            T = 1;

            this.numSuccessForAction = numSuccessForAction;
            this.numRequestForAction = numRequestForAction;
        }

        public void TrainAgent(int numberOfIterations)
        {
            for (int i = 0; i < numberOfIterations; i++)
            {
                //lấy init state bất kỳ
                int initialState = RandomInitialState();
                //tính value cho q table
                InitializeEpisode(initialState);
            }
        }

        public int Run(int initialState)
        {
            if (initialState < 0 || initialState > _qLearningProblem.NumberOfStates) 
                throw new ArgumentException($"The initial state can be between [0-{_qLearningProblem.NumberOfStates}", nameof(initialState));
            //return SelectAction(initialState);
            return UCBSelectAction(T,initialState);
        }

        /// <summary>
        /// Dùng cho train và lựa chọn
        /// </summary>
        /// <param name="currentState"></param>
        /// <returns></returns>
        public int SelectAction(int currentState)
        {
            double n = _random.NextDouble();
            int action;
            if (n < _epsilon)
            {
                var div = numSuccessForAction / (double)numRequestForAction;
                if (div <= 0.5)
                {
                    action = _random.Next(0, 2);
                }
                else if (div > 0.5 && div < 1)
                {
                    action = _random.Next(2, 4);
                }
                else //== 1
                {
                    action = _random.Next(4, 6);
                }
            }
            else
            {
                var qValueMax = _qTable[currentState].Max();
                action = _qTable[currentState].ToList().IndexOf(qValueMax);
            }

            T++; //đếm số lần pull
            return action;
        }

        public int UCBSelectAction(int t, int currentState)
        {
            var bestAction = 0;
            var sa = new StateAndAction { State = currentState, Action = 0 };
            if (!_nPull.ContainsKey(sa))
            {
                _nPull.Add(sa, 1);
            }
            var bestUCB = _qTable[currentState][0] + _c * Math.Sqrt(Math.Log(t) / _nPull[sa]);
            for (int i = 1; i < 6; i++)
            {
                sa = new StateAndAction { State = currentState, Action = i };
                if (!_nPull.ContainsKey(sa))
                {
                    _nPull.Add(sa, 1);
                }
                var ucb = _qTable[currentState][0] + _c * Math.Sqrt(Math.Log(t) / _nPull[sa]);
                if (ucb > bestUCB)
                {
                    bestUCB = ucb;
                    bestAction = i;
                }
            }
            _nPull[sa]++; //tăng pull lên
            return bestAction;
        }

        /// <summary>
        /// Tính value cho q table
        /// </summary>
        /// <param name="initialState">trạng thái khởi tạo</param>
        private void InitializeEpisode(int initialState)
        {
            int currentState = initialState;
            for (int i = 0; i < 6; i++)
            {
                TakeAction(currentState);
            }
        }

        /// <summary>
        /// Lấy ra action theo quy tắc và tính q value
        /// </summary>
        /// <param name="currentState">trạng thái đang xét</param>
        private void TakeAction(int currentState)
        {
            //random action
            //int action = SelectAction(currentState);
            int action = UCBSelectAction(T, currentState);

            //lấy ra giá trị reward tại s và a chỉ định
            double saReward = _qLearningProblem.GetReward(currentState, action);

            //lấy max đã có tại action đang xét
            double maxQValue = _qTable[action].Max();

            //tính ra value mới
            double qCurrentState = _qTable[currentState][action] + _alpha * (saReward + _gamma * maxQValue - _qTable[currentState][action]);

            //cập nhật vào q table tại s(curr) a(random)
            _qTable[currentState][action] = qCurrentState;
        }

        /// <summary>
        /// Random ra state để train
        /// </summary>
        /// <param name="numberOfStates">Số state</param>
        /// <returns></returns>
        private int RandomInitialState()
        {
            if (_logState.Length > 0)
            {
                int index = _random.Next(0, _logState.Length);
                var state = _logState[index];
                return _qLearningProblem.GetState(state.l1, state.l2, state.NOE);
            }
            return _random.Next(0, _qLearningProblem.NumberOfStates);
        }

        public string ShowQTable()
        {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < _qLearningProblem.NumberOfStates; i++)
            {
                for (int j = 0; j < _qLearningProblem.NumberOfActions; j++)
                {
                    if (_qTable[i][j] != 0)
                    {
                        sb.Append($"{i}: {_qTable[i][0]} {_qTable[i][1]} {_qTable[i][2]} {_qTable[i][3]} {_qTable[i][4]} {_qTable[i][5]}\n");
                        break;
                    }
                }
            }
            return sb.ToString();
        }
    }
}
