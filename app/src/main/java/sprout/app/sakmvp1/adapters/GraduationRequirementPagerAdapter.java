package sprout.app.sakmvp1.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import sprout.app.sakmvp1.fragments.CreditRequirementsFragment;
import sprout.app.sakmvp1.fragments.GeneralCoursesFragment;
import sprout.app.sakmvp1.fragments.MajorCoursesFragment;
import sprout.app.sakmvp1.fragments.ReplacementRulesFragment;

/**
 * 졸업요건 편집 ViewPager2 Adapter
 * 4개의 탭(학점요건, 전공과목, 교양과목, 대체과목)을 관리
 */
public class GraduationRequirementPagerAdapter extends FragmentStateAdapter {

    private CreditRequirementsFragment creditFragment;
    private MajorCoursesFragment majorFragment;
    private GeneralCoursesFragment generalFragment;
    private ReplacementRulesFragment replacementFragment;

    public GraduationRequirementPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                creditFragment = new CreditRequirementsFragment();
                return creditFragment;
            case 1:
                majorFragment = new MajorCoursesFragment();
                return majorFragment;
            case 2:
                generalFragment = new GeneralCoursesFragment();
                return generalFragment;
            case 3:
                replacementFragment = new ReplacementRulesFragment();
                return replacementFragment;
            default:
                return new CreditRequirementsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    /**
     * 각 Fragment에 대한 접근자 메서드
     */
    public CreditRequirementsFragment getCreditFragment() {
        return creditFragment;
    }

    public MajorCoursesFragment getMajorFragment() {
        return majorFragment;
    }

    public GeneralCoursesFragment getGeneralFragment() {
        return generalFragment;
    }

    public ReplacementRulesFragment getReplacementFragment() {
        return replacementFragment;
    }
}
