README - Card Game Test Suite
================================

1. HOW TO RUN THE TEST SUITE
Prerequisites:
Java 8 or higher
JUnit 5 framework

Test Files Location:
All test classes are in the 'cardgame.test' package
Test classes: CardTest.java, CardDeckTest.java, PlayerTest.java

2. Running Tests in Eclipse
Option 1: Run All Tests
1) Right-click on the 'cardgame.test' package in Package Explorer
2) Select 'Run As' -> 'JUnit Test'
3) All 18 tests should pass (6 + 7 + 5)

Option 2: Run Individual Test Classes
1) Right-click on a specific test class (e.g., CardTest.java)
2) Select 'Run As' -> 'JUnit Test'

3. Test Coverage
1) CardTest (6 tests):
- testCardCreation
- testCardWithZeroDenomination
- testCardWithLargeDenomination
- testCardToString
- testCardEquality
- testCardHashCode
2) CardDeckTest (7 tests):
- testDeckCreation
- testAddCard
- testDrawCard
- testFIFOOrder
- testToString
- testConcurrentAdd
- testConcurrentDrawAndAdd
3) PlayerTest (5 tests):
- testPlayerCreation
- testAddCard
- testHasWinningHand
- testInitialHandLogging
- testNotifyGameWon

4. Expected Results
All 18 tests should pass with green indicators.

5. Test Pack Files
The following pack files are provided for testing:
1) two.txt: Pack for 2 players (16 cards)
2) three.txt: Pack for 3 players (24 cards)
3) four.txt: Pack for 4 players (32 cards)

6. Running the Main Program
1) From JAR file:
  java -jar cards.jar
2) From source:
a. Run CardGame.java in Eclipse
b. Enter number of players
c. Enter pack file location
d. Observe game output

7. Output Files
After running the game, the following files are generated:
a. player{i}_output.txt (for each player i)
b. deck{i}_output.txt (for each deck i)
These files should be deleted between test runs.