mkdir .claude

echo "--------------------------------------------"
echo "Adding copilot instructions"
echo "--------------------------------------------"

git submodule add https://github.com/Adrixan/copilot-instructions.git .github/copilot-instructions
ln -s copilot-instructions/copilot-instructions.md .github/copilot-instructions.md
git add .gitmodules .github/

echo "--------------------------------------------"
echo "Adding Kilo Code instructions"
echo "--------------------------------------------"

# Add the submodule
git submodule add https://github.com/Adrixan/kilo-glm5-instructions.git .kilocode/glm5-instructions

# Run the installation script
./.kilocode/glm5-instructions/install.sh

echo "--------------------------------------------"
echo "Adding impeccable skills"
echo "--------------------------------------------"

git submodule add https://github.com/pbakaus/impeccable.git .impeccable
mkdir -p .claude/skills/impeccable
mkdir -p .github/skills/impeccable
mkdir -p .kilocode/skills/impeccable
ln -s .impeccable/.claude/skills .claude/skills/impeccable
ln -s .impeccable/.claude/skills .github/skills/impeccable
ln -s .impeccable/.claude/skills .kilocode/skills/impeccable

echo "--------------------------------------------"
echo "Adding MIniMax Skills"
echo "--------------------------------------------"

git submodule add https://github.com/MiniMax-AI/skills.git .minimax
mkdir -p .claude/skills/minimax
mkdir -p .github/skills/minimax
mkdir -p .kilcode/skills/minimax
ln -s .minimax/skills/ .claude/skills/minimax
ln -s .minimax/skills/ .github/skills/minimax
ln -s .minimax/skills/ .kilocode/skills/minimax

echo "--------------------------------------------"
echo "Adding the agency"
echo "--------------------------------------------"

git submodule add https://github.com/msitarzewski/agency-agents.git .agency

mkdir -p .claude/agents/agency
mkdir -p .github/agents/agency
mkdir -p .kilocode/agents/agency

ln -s .agency/ .claude/agents/agency
ln -s .agency/ .github/agents/agency
ln -s .agency/ .kilocode/agents/agency

# git add .claude/ .github/ .kilocode

echo "--------------------------------------------"
echo "Initiation finished"
echo "--------------------------------------------"
