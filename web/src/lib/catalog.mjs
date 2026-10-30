const normalize = (value) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("pt-BR");

export function filterExercises(
  catalog,
  query = "",
  muscle = "Todos",
  equipment = "Todos",
) {
  const terms = normalize(query).trim().split(/\s+/).filter(Boolean);
  return catalog.filter((exercise) => {
    const text = normalize(
      `${exercise.name} ${exercise.muscle} ${exercise.equipment}`,
    );
    return (
      (muscle === "Todos" || exercise.muscle === muscle) &&
      (equipment === "Todos" || exercise.equipment === equipment) &&
      terms.every((term) => text.includes(term))
    );
  });
}
