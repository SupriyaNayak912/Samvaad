import pool from '../config/database';
});
  process.exit(1);
  console.error('Seed failed:', err);
seedScenarios().catch((err) => {
// Run seed

}
  }
    throw error;
    console.error('❌ Error seeding scenarios:', error);
  } catch (error) {
    console.log('✅ Sample scenarios seeded successfully');

    }
      }
        console.log(`✓ Seeded: ${scenario.title}`);
        );
          ]
            scenario.tips,
            scenario.duration_seconds,
            scenario.difficulty,
            scenario.category,
            scenario.description,
            scenario.title,
          [
           VALUES ($1, $2, $3, $4, $5, $6)`,
          `INSERT INTO scenarios (title, description, category, difficulty, duration_seconds, tips)
        await pool.query(
      if (existing.rows.length === 0) {

      );
        [scenario.title]
        'SELECT id FROM scenarios WHERE title = $1',
      const existing = await pool.query(
    for (const scenario of scenarios) {

    ];
      },
        tips: 'Provide specific examples of handling pressure',
        duration_seconds: 120,
        difficulty: 'Medium',
        category: 'Behavioral',
        description: 'Demonstrate resilience and coping strategies',
        title: 'How do you handle stress and pressure?',
      {
      },
        tips: 'Be authentic while showing flexibility',
        duration_seconds: 120,
        difficulty: 'Easy',
        category: 'Culture Fit',
        description: 'Discuss what helps you perform best',
        title: 'Describe your ideal work environment',
      {
      },
        tips: 'Align your goals with the company\'s growth',
        duration_seconds: 120,
        difficulty: 'Medium',
        category: 'Career Planning',
        description: 'Share your career aspirations',
        title: 'Where do you see yourself in 5 years?',
      {
      },
        tips: 'Show self-awareness and growth mindset',
        duration_seconds: 120,
        difficulty: 'Medium',
        category: 'Behavioral',
        description: 'Discuss areas for improvement professionally',
        title: 'What is your weakness?',
      {
      },
        tips: 'Be honest and provide examples',
        duration_seconds: 120,
        difficulty: 'Easy',
        category: 'Behavioral',
        description: 'Highlight your key competencies',
        title: 'What are your strengths?',
      {
      },
        tips: 'Use the STAR method (Situation, Task, Action, Result)',
        duration_seconds: 180,
        difficulty: 'Hard',
        category: 'Problem Solving',
        description: 'Discuss how you handled a difficult situation',
        title: 'Describe a challenging project',
      {
      },
        tips: 'Research the company thoroughly before answering',
        duration_seconds: 120,
        difficulty: 'Medium',
        category: 'Motivation',
        description: 'Understanding your motivation for the role',
        title: 'Why do you want to work here?',
      {
      },
        tips: 'Keep it concise and relevant to the job',
        duration_seconds: 120,
        difficulty: 'Easy',
        category: 'Behavioral',
        description: 'A common introductory question in interviews',
        title: 'Tell me about yourself',
      {
    const scenarios = [
  try {
export async function seedScenarios() {

